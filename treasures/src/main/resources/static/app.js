/*
 * Treasures of the Oceans - browser client.
 *
 * The server engine is a state machine; this file is a thin driver for it. Every
 * response carries a `status`, and that status alone decides what may be called next:
 *
 *   AWAITING_NEXT_ROUND    -> POST /advance   (and only then)
 *   AWAITING_PLAYER_CHOICE -> POST /choose    (and only then)
 *   GAME_OVER              -> nothing; show the overlay
 *
 * Calling out of turn earns a 409 from the engine's own guards, so the loop below never
 * guesses - it reads the status back after every call and branches on it.
 *
 * All paths are relative, so the page works on whatever port happens to serve it.
 */

const ROUND_DELAY_MS = 900;
const STATS = ['Speed', 'Size', 'Danger'];

/* ------------------------------------------------------------------ state */

const state = {
  gameId: null,
  view: null,      // the most recent GameStateView
  roster: [],      // [{name, human}] captured at creation, in seat order
  busy: false,     // single in-flight guard: no two requests, no two loops
  loopToken: 0     // bumped to abandon a loop that is still awaiting a delay
};

/* -------------------------------------------------------------- transport */

class ApiFailure extends Error {
  constructor(message, status) {
    super(message);
    this.status = status;
  }
}

/** fetch + JSON, turning any non-2xx into an ApiFailure carrying the server's message. */
async function api(method, path, body) {
  const options = { method, headers: {} };
  if (body !== undefined) {
    options.headers['Content-Type'] = 'application/json';
    options.body = JSON.stringify(body);
  }

  let response;
  try {
    response = await fetch(path, options);
  } catch (networkError) {
    throw new ApiFailure('Could not reach the server. Is it still running?', 0);
  }

  if (response.status === 204) return null;

  const text = await response.text();
  let data = null;
  if (text) {
    try { data = JSON.parse(text); } catch (ignored) { /* leave null; handled below */ }
  }

  if (!response.ok) {
    // ApiExceptionHandler returns {status, error, message}; fall back if it didn't.
    const message = (data && data.message) ? data.message
                  : (text || (response.status + ' ' + response.statusText));
    throw new ApiFailure(message, response.status);
  }
  return data;
}

const delay = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

/* --------------------------------------------------------------- helpers */

const $ = (id) => document.getElementById(id);

function showError(message) {
  $('error-text').textContent = message;
  $('error-strip').hidden = false;
}

function clearError() {
  $('error-strip').hidden = true;
}

/**
 * The last card each player put down.
 * A war recurses server-side and APPENDS to the same reveals list, so a player can
 * appear more than once; the later entry is the card that actually stands.
 */
function lastRevealPerPlayer(round) {
  const byPlayer = new Map();
  if (round && Array.isArray(round.reveals)) {
    for (const reveal of round.reveals) byPlayer.set(reveal.playerName, reveal);
  }
  return byPlayer;
}

/* --------------------------------------------------------------- setup UI */

function renderSeats() {
  const count = Number($('player-count').value);
  const seats = $('seats');
  const previous = readSeats();
  seats.innerHTML = '';

  for (let i = 0; i < count; i++) {
    const row = document.createElement('div');
    row.className = 'seat';

    const label = document.createElement('span');
    label.className = 'seat-index';
    label.textContent = 'Seat ' + (i + 1);

    const type = document.createElement('select');
    type.className = 'seat-type';
    const kinds = [['HUMAN', 'Human'], ['EASY_BOT', 'Easy Bot'],
                   ['MEDIUM_BOT', 'Medium Bot'], ['HARD_BOT', 'Hard Bot']];
    for (const pair of kinds) {
      const option = document.createElement('option');
      option.value = pair[0];
      option.textContent = pair[1];
      type.appendChild(option);
    }
    // Sensible default: seat 1 is you, the rest are bots. Keep prior picks on re-render.
    type.value = previous[i] ? previous[i].type : (i === 0 ? 'HUMAN' : 'EASY_BOT');

    const name = document.createElement('input');
    name.type = 'text';
    name.className = 'seat-name';
    name.placeholder = 'Player ' + (i + 1);
    name.maxLength = 24;
    if (previous[i]) name.value = previous[i].name;

    row.append(label, type, name);
    seats.appendChild(row);
  }

  const deckSize = $('deck-size');
  deckSize.min = String(count);
  if (Number(deckSize.value) < count) deckSize.value = String(count);
  updateDeckHelp();
}

function readSeats() {
  return Array.from(document.querySelectorAll('.seat')).map((row) => ({
    type: row.querySelector('.seat-type').value,
    name: row.querySelector('.seat-name').value
  }));
}

function updateDeckHelp() {
  const count = Number($('player-count').value);
  const size = Number($('deck-size').value);
  const each = Number.isFinite(size) && size >= count ? Math.floor(size / count) : 0;
  $('deck-help').textContent =
    'Between ' + count + ' and 60 cards (at least one per player). '
    + (each ? each + ' cards each; any remainder is left out of play.' : '');
}

/* ------------------------------------------------------------ card render */

/**
 * One playing card. `reveal` is a Reveal: {playerName, cardName, speed, size, danger}.
 * The media block is a fixed slot held open for the artwork a future Card.imagePath will
 * supply - filling it in later must not reflow anything around it.
 */
function cardElement(reveal, options) {
  const opts = options || {};
  const winner = !!opts.winner;
  const playedStat = opts.playedStat || null;
  const faceDown = !!opts.faceDown;
  const owner = opts.owner || null;

  const card = document.createElement('div');
  card.className = 'card' + (winner ? ' is-winner' : '') + (faceDown ? ' is-facedown' : '');

  if (faceDown) {
    const blankTitle = document.createElement('div');
    blankTitle.className = 'card-title';
    blankTitle.innerHTML = '&nbsp;';

    const back = document.createElement('div');
    back.className = 'card-media card-media--back';

    const blankStats = document.createElement('div');
    blankStats.className = 'card-stats';

    card.append(blankTitle, back, blankStats);
  } else {
    const title = document.createElement('div');
    title.className = 'card-title';
    title.textContent = reveal.cardName;

    const media = document.createElement('div');
    media.className = 'card-media';
    media.setAttribute('aria-hidden', 'true');   // decorative until real artwork lands

    const stats = document.createElement('div');
    stats.className = 'card-stats';
    for (const stat of STATS) {
      const row = document.createElement('div');
      row.className = 'stat-row' + (playedStat === stat ? ' is-played' : '');
      const key = document.createElement('span');
      key.className = 'stat-key';
      key.textContent = stat;
      const value = document.createElement('span');
      value.className = 'stat-value';
      value.textContent = reveal[stat.toLowerCase()];
      row.append(key, value);
      stats.appendChild(row);
    }

    card.append(title, media, stats);
  }

  if (owner) {
    const foot = document.createElement('div');
    foot.className = 'card-owner';
    foot.textContent = owner;
    card.appendChild(foot);
  }
  return card;
}

/* ----------------------------------------------------------- table render */

function render() {
  const view = state.view;
  if (!view) return;

  const round = view.lastRound;
  const played = lastRevealPerPlayer(round);

  /* turn banner */
  const banner = $('turn-banner');
  if (view.status === 'GAME_OVER') {
    banner.textContent = 'Game over';
  } else if (view.status === 'AWAITING_PLAYER_CHOICE') {
    banner.textContent = view.turnHolder + ' - your call';
  } else {
    banner.textContent = view.turnHolder ? view.turnHolder + ' is choosing...' : '';
  }

  /* what just happened */
  const note = $('round-note');
  if (round) {
    const war = round.war ? ' - WAR! tied players replayed' : '';
    const won = round.winnerName ? round.winnerName + ' takes the pile' : 'unresolved';
    note.textContent = round.chooserName + ' played ' + round.stat + ' - ' + won + war;
  } else {
    note.textContent = 'No cards played yet.';
  }

  /* the table: seats stay in a fixed order so cards do not jump between rounds */
  const table = $('table-cards');
  table.innerHTML = '';
  for (const seat of state.roster) {
    const reveal = played.get(seat.name);
    const stillIn = view.players.some((p) => p.name === seat.name);

    const slot = document.createElement('div');
    slot.className = 'table-slot' + (stillIn ? '' : ' is-out');

    if (reveal) {
      slot.appendChild(cardElement(reveal, {
        winner: !!round && reveal.playerName === round.winnerName,
        playedStat: round ? round.stat : null,
        owner: seat.name
      }));
    } else {
      slot.appendChild(cardElement(null, { faceDown: true, owner: seat.name }));
    }
    table.appendChild(slot);
  }

  /* stat buttons, strictly gated on status - blind play: the human's own card is never
     shown before the choice, it appears in lastRound.reveals once the round resolves */
  const choiceArea = $('choice-area');
  if (view.status === 'AWAITING_PLAYER_CHOICE') {
    choiceArea.hidden = false;
    setStatButtons(true);
  } else {
    choiceArea.hidden = true;
    setStatButtons(false);
  }

  /* roster - eliminated players drop out of players[] entirely, so compare against the
     roster captured at setup rather than looking for a cardsRemaining of 0 */
  const roster = $('roster');
  roster.innerHTML = '';
  for (const seat of state.roster) {
    const live = view.players.find((p) => p.name === seat.name);
    const isTurn = !!live && view.turnHolder === seat.name && view.status !== 'GAME_OVER';

    const item = document.createElement('li');
    item.className = 'roster-row' + (live ? '' : ' is-eliminated') + (isTurn ? ' is-turn' : '');

    const who = document.createElement('span');
    who.className = 'roster-name';
    who.textContent = seat.name;

    const kind = document.createElement('span');
    kind.className = 'roster-kind';
    kind.textContent = seat.human ? 'you' : 'bot';

    const cards = document.createElement('span');
    cards.className = 'roster-cards';
    cards.textContent = live ? live.cardsRemaining + ' cards' : 'eliminated';

    item.append(who, kind, cards);
    roster.appendChild(item);
  }
}

function setStatButtons(enabled) {
  const buttons = document.querySelectorAll('.stat-btn');
  for (const button of buttons) button.disabled = !enabled;
}

/* -------------------------------------------------------------- play loop */

/**
 * Drive bot rounds until the engine needs a human or the game ends.
 * Guarded three ways: `busy` stops a second loop starting, `loopToken` abandons a loop
 * whose game has been discarded mid-delay, and the while condition means /advance is
 * only ever POSTed while the status genuinely is AWAITING_NEXT_ROUND.
 */
async function playLoop() {
  if (state.busy) return;
  state.busy = true;
  const token = state.loopToken;

  try {
    while (state.view && state.view.status === 'AWAITING_NEXT_ROUND') {
      const next = await api('POST', '/api/games/' + state.gameId + '/advance');
      if (token !== state.loopToken) return;        // a new game started while we waited

      state.view = next;
      render();
      if (next.status !== 'AWAITING_NEXT_ROUND') break;

      await delay(ROUND_DELAY_MS);
      if (token !== state.loopToken) return;
    }
  } catch (error) {
    showError(error.message);
    return;
  } finally {
    state.busy = false;
  }

  if (token === state.loopToken && state.view && state.view.status === 'GAME_OVER') {
    showGameOver();
  }
}

async function onChooseStat(stat) {
  if (state.busy) return;
  if (!state.view || state.view.status !== 'AWAITING_PLAYER_CHOICE') return;

  state.busy = true;
  setStatButtons(false);
  const token = state.loopToken;

  try {
    state.view = await api('POST', '/api/games/' + state.gameId + '/choose', { stat: stat });
    if (token !== state.loopToken) return;
    clearError();
    render();
  } catch (error) {
    showError(error.message);
    setStatButtons(true);
    return;
  } finally {
    state.busy = false;
  }

  if (token !== state.loopToken) return;
  if (state.view.status === 'GAME_OVER') showGameOver();
  else playLoop();
}

/* ---------------------------------------------------------------- screens */

function showGameOver() {
  const winner = state.view ? state.view.winnerName : null;
  $('winner-line').textContent = winner ? winner + ' wins' : 'Nobody wins - a draw';
  $('gameover').hidden = false;
}

async function onStart() {
  clearError();
  const seats = readSeats();

  // Names identify players in every later payload, so make sure they are unique -
  // two "Player 1"s would make the winner highlight and the roster ambiguous.
  const used = new Set();
  const players = seats.map((seat, i) => {
    const base = seat.name.trim() || ('Player ' + (i + 1));
    let candidate = base;
    let n = 2;
    while (used.has(candidate)) {
      candidate = base + ' (' + n + ')';
      n++;
    }
    used.add(candidate);
    return { type: seat.type, name: candidate };
  });

  const config = { deckSize: Number($('deck-size').value), players: players };

  $('start-btn').disabled = true;
  try {
    const created = await api('POST', '/api/games', config);
    state.gameId = created.gameId;
    state.view = created.state;
    // Take the roster from the server's own reply so the names match byte for byte.
    state.roster = created.state.players.map((p) => ({ name: p.name, human: p.human }));

    $('setup').hidden = true;
    $('table-screen').hidden = false;
    render();
    playLoop();
  } catch (error) {
    showError(error.message);           // e.g. 400 "A game needs at least 2 players."
  } finally {
    $('start-btn').disabled = false;
  }
}

async function onNewGame() {
  state.loopToken++;          // any loop still sleeping will bail out on wake
  const finished = state.gameId;

  state.gameId = null;
  state.view = null;
  state.roster = [];
  state.busy = false;

  $('gameover').hidden = true;
  $('table-screen').hidden = true;
  $('setup').hidden = false;
  $('table-cards').innerHTML = '';
  $('roster').innerHTML = '';
  clearError();

  if (finished) {
    try {
      await api('DELETE', '/api/games/' + finished);
    } catch (ignored) {
      /* the game is already unreachable from here; nothing to recover */
    }
  }
}

/* ------------------------------------------------------------------ wiring */

$('player-count').addEventListener('change', renderSeats);
$('deck-size').addEventListener('input', updateDeckHelp);
$('start-btn').addEventListener('click', onStart);
$('new-game-btn').addEventListener('click', onNewGame);
$('error-dismiss').addEventListener('click', clearError);
for (const button of document.querySelectorAll('.stat-btn')) {
  button.addEventListener('click', () => onChooseStat(button.dataset.stat));
}

renderSeats();

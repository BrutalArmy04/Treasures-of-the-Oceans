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

// How long to pause between auto-played bot rounds. One object so the pacing is
// trivial to retune without hunting through the loop.
const BOT_DELAYS_MS = { slow: 1600, normal: 900, fast: 400 };
const DEFAULT_BOT_MODE = 'manual';
const DEFAULT_BOT_SPEED = 'normal';
const STATS = ['Speed', 'Size', 'Danger'];

/* ------------------------------------------------------------------ state */

const state = {
  gameId: null,
  view: null,      // the most recent GameStateView
  roster: [],      // [{name, human}] captured at creation, in seat order
  busy: false,     // single in-flight guard: no two requests, no two loops
  loopToken: 0,    // bumped to abandon a loop that is still awaiting a delay

  // Bot pacing. Both the setup screen and the live table control read and write
  // exactly these two fields - there is no parallel copy - and the play loop reads
  // them fresh on every iteration, so a change lands on the next bot round.
  botMode: DEFAULT_BOT_MODE,    // 'manual' -> one round per click | 'auto' -> self-advancing
  botSpeed: DEFAULT_BOT_SPEED   // key into BOT_DELAYS_MS
};

const botDelayMs = () => BOT_DELAYS_MS[state.botSpeed] || BOT_DELAYS_MS.normal;

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

  renderControls();

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
  // Scoped to the stat block: the Next-round button must never be swept by this.
  const buttons = document.querySelectorAll('#stat-choice .stat-btn');
  for (const button of buttons) button.disabled = !enabled;
}

/** The player object whose turn it is, or null. Straight off the view - no extra request. */
function turnHolder(view) {
  if (!view || !view.turnHolder || !Array.isArray(view.players)) return null;
  return view.players.find((p) => p.name === view.turnHolder) || null;
}

/**
 * AWAITING_NEXT_ROUND means "a round is pending for the turn holder" - and that holder
 * is the HUMAN at game start and after every round they win. The human must never be
 * left staring at their own pending round, so it is always stepped through: the engine's
 * advance() for a human only flips the status, consuming no card.
 */
function humanRoundPending(view) {
  const holder = turnHolder(view);
  return !!view && view.status === 'AWAITING_NEXT_ROUND' && !!holder && holder.human;
}

/**
 * Whether /advance may be POSTed right now. Two reasons to advance, one gate:
 *   - the pending round belongs to the human -> always step it (regardless of botMode)
 *   - the pending round belongs to a bot     -> only in auto mode; manual waits for a click
 * The status term is what keeps /advance strictly legal, so this is the only test the
 * loop needs.
 */
function shouldAdvance() {
  const view = state.view;
  if (!view || view.status !== 'AWAITING_NEXT_ROUND') return false;
  return humanRoundPending(view) || state.botMode === 'auto';
}

/**
 * The control strip under the table. Everything here is derived from status +
 * state.botMode, never from a click history, so it is always consistent with the
 * engine:
 *   AWAITING_PLAYER_CHOICE -> stat buttons (the human's blind choice)
 *   AWAITING_NEXT_ROUND    -> "Next round" in manual mode; nothing in auto (the loop drives)
 *   GAME_OVER              -> the whole strip goes away; the overlay takes over
 * `busy` disables the live buttons so an in-flight request cannot be double-fired.
 */
function renderControls() {
  const view = state.view;
  const choiceArea = $('choice-area');

  if (!view || view.status === 'GAME_OVER') {
    choiceArea.hidden = true;
    setStatButtons(false);
    $('next-round-btn').hidden = true;
    return;
  }

  const humanTurn = view.status === 'AWAITING_PLAYER_CHOICE';
  // A pending round is only a BOT's turn if the holder is actually a bot - the human
  // holds it at game start and after every win, and must not be offered "Next round".
  const holder = turnHolder(view);
  const botTurn = view.status === 'AWAITING_NEXT_ROUND' && !!holder && !holder.human;

  choiceArea.hidden = false;                 // it also carries the live bot controls now
  $('stat-choice').hidden = !humanTurn;
  setStatButtons(humanTurn && !state.busy);

  // Only ever on a bot's turn in manual mode. On the human's turn the stat buttons
  // are the way forward, so Next round must not be offered as a second route.
  const offerNext = botTurn && state.botMode === 'manual';
  const nextBtn = $('next-round-btn');
  nextBtn.hidden = !offerNext;
  nextBtn.disabled = !offerNext || state.busy;

  syncBotControls();
}

/** Push state.botMode/botSpeed back into BOTH control sets so they never disagree. */
function syncBotControls() {
  $('bot-mode').value = state.botMode;
  $('bot-speed').value = state.botSpeed;
  $('table-bot-mode').value = state.botMode;
  $('table-bot-speed').value = state.botSpeed;
}

/**
 * Mode changes are handled through the shared state and the existing loopToken/busy
 * guards - never by starting a second loop.
 *   manual -> auto : kick playLoop() so it picks up from wherever it paused. If a
 *                    request is already in flight, `busy` makes this a no-op and the
 *                    running loop simply keeps going, since it re-reads botMode.
 *   auto -> manual : nothing to cancel. The loop re-checks botMode at the top of each
 *                    iteration, so it finishes the round in flight and then stops.
 */
function setBotMode(mode) {
  state.botMode = mode === 'auto' ? 'auto' : 'manual';
  syncBotControls();
  if (state.botMode === 'auto') playLoop();
  else renderControls();
}

function setBotSpeed(speed) {
  state.botSpeed = BOT_DELAYS_MS[speed] ? speed : DEFAULT_BOT_SPEED;
  syncBotControls();      // the loop reads botDelayMs() per round, so this needs no restart
}

/* -------------------------------------------------------------- play loop */

/**
 * Drive bot rounds until the engine needs a human, the game ends, or the player has
 * asked to step through them by hand.
 *
 * Guarded four ways: `busy` stops a second loop starting, `loopToken` abandons a loop
 * whose game has been discarded mid-delay, the while condition means /advance is only
 * ever POSTed while the status genuinely is AWAITING_NEXT_ROUND, and botMode is
 * re-read every iteration so flipping to manual stops the loop after the round that
 * is already in flight - never mid-request.
 *
 * In manual mode this is a no-op that just repaints; onNextRound() does the stepping.
 */
async function playLoop() {
  if (state.busy) return;
  if (!shouldAdvance()) { renderControls(); return; }   // park on "Next round", or repaint

  state.busy = true;
  const token = state.loopToken;

  try {
    // shouldAdvance() is re-evaluated every iteration, so flipping to manual stops the
    // loop after the round in flight, and a human-held pending round is always stepped.
    while (shouldAdvance()) {
      const next = await api('POST', '/api/games/' + state.gameId + '/advance');
      if (token !== state.loopToken) return;        // a new game started while we waited

      state.view = next;
      render();
      if (!shouldAdvance()) break;                  // human's choice, game over, or manual

      await delay(botDelayMs());                    // speed is read fresh each round
      if (token !== state.loopToken) return;
    }
  } catch (error) {
    showError(error.message);
    return;
  } finally {
    state.busy = false;
  }

  if (token !== state.loopToken || !state.view) return;
  if (state.view.status === 'GAME_OVER') showGameOver();
  else renderControls();      // repaint now busy is clear: re-enables the live buttons
}

/**
 * One bot round, by hand. Same status gate as the loop, so /advance still cannot be
 * called out of turn, and `busy` plus the explicit disable make a double-click a no-op.
 */
async function onNextRound() {
  if (state.busy) return;
  if (!state.view || state.view.status !== 'AWAITING_NEXT_ROUND') return;

  state.busy = true;
  $('next-round-btn').disabled = true;
  const token = state.loopToken;

  try {
    const next = await api('POST', '/api/games/' + state.gameId + '/advance');
    if (token !== state.loopToken) return;
    state.view = next;
    clearError();
    render();
  } catch (error) {
    showError(error.message);
    return;
  } finally {
    state.busy = false;
  }

  if (token !== state.loopToken) return;
  if (state.view.status === 'GAME_OVER') showGameOver();
  // One clean repaint with busy already cleared - without it a fresh human turn keeps
  // the disabled state painted during the request. Then hand back to the loop, which
  // steps a human-held pending round or resumes auto.
  else { renderControls(); playLoop(); }
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

  // Seed the shared settings from the setup screen; the table controls edit these same
  // two fields from here on.
  state.botMode = $('bot-mode').value === 'auto' ? 'auto' : 'manual';
  state.botSpeed = BOT_DELAYS_MS[$('bot-speed').value] ? $('bot-speed').value : DEFAULT_BOT_SPEED;

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
$('next-round-btn').addEventListener('click', onNextRound);

// Setup and table controls are two views of the same two state fields.
$('bot-mode').addEventListener('change', (e) => { state.botMode = e.target.value; syncBotControls(); });
$('bot-speed').addEventListener('change', (e) => setBotSpeed(e.target.value));
$('table-bot-mode').addEventListener('change', (e) => setBotMode(e.target.value));
$('table-bot-speed').addEventListener('change', (e) => setBotSpeed(e.target.value));
// Scoped to the stat block so the Next-round button can never pick up a stat handler.
for (const button of document.querySelectorAll('#stat-choice .stat-btn')) {
  button.addEventListener('click', () => onChooseStat(button.dataset.stat));
}

renderSeats();
syncBotControls();

// Codul sursa este adaptat dupa OpenGLBook.com

#define _CRT_SECURE_NO_WARNINGS
#include <windows.h>  // biblioteci care urmeaza sa fie incluse
#include <stdlib.h> // necesare pentru citirea shader-elor
#include <stdio.h>
#include <time.h>
#include <cmath>
#include <GL/glew.h> // glew apare inainte de freeglut
#include <GL/freeglut.h> // nu trebuie uitat freeglut.h
#include "loadShaders.h"
#include "glm/glm.hpp"
#include "glm/gtc/matrix_transform.hpp"
#include "SOIL.h"


//////////////////////////////////////

GLuint
VaoId,
VboId,
EboId,
BackgroundVaoId,
BackgroundVboId,
BackgroundProgramId,
BannerVaoId,
BannerVboId,
BannerEboId,
ProgramId,
myMatrixLocation,
textureLocation,
highlightRowLocation,
winnerLocation;

glm::mat4 myMatrix, resizeMatrix, translateMatrix, rotateMatrix, scaleMatrix, flipMatrix;

const float PI = 3.14159265f;
const int MAX_CARDS = 64;
const int MAX_TWEENS = 128;
const int CARDS_PER_PLAYER = 5;
const int HUMAN = 0;
const int BOT = 1;

const float FLIP_DURATION = 600.0f;
const float MOVE_DURATION = 550.0f;
const float SCALE_DURATION = 600.0f;
const float DEAL_GAP = 160.0f;
const int SHOW_DURATION = 1500;

const float PILE_SCALE = 0.8f;
const float TABLE_SCALE = 1.6f;
const float PILE_JITTER_ANGLE = 0.08f;
const float PILE_JITTER_POS = 3.0f;
const float DECK_X = -320.0f, DECK_Y = 0.0f;
const float PILE_X[2] = { 0.0f, 0.0f };
const float PILE_Y[2] = { -300.0f, 300.0f };
const float TABLE_X[2] = { -110.0f, 110.0f };
const float TABLE_Y[2] = { 0.0f, 0.0f };
const float WAR_OFFSET_X = 22.0f;
const float WAR_OFFSET_Y = 22.0f;
const float WAR_JITTER_ANGLE = 0.07f;
const float BANNER_WIDTH = 560.0f;
const float BANNER_HEIGHT = BANNER_WIDTH * 64.0f / 640.0f;
const float BANNER_Y = -178.0f;
const float BG_TOP[3] = { 0.420f, 0.769f, 0.722f };
const float BG_MIDDLE[3] = { 0.643f, 0.784f, 0.635f };
const float BG_BOTTOM[3] = { 0.851f, 0.729f, 0.510f };

const char* STAT_NAMES[3] = { "Speed", "Size", "Danger" };
const char* PLAYER_NAMES[2] = { "Tu", "Botul" };

struct CardState
{
	float x, y, angle, scale, theta;
};

struct Card
{
	int id;
	char name[64];
	int speed, size, danger;
	CardState state;
};

struct Tween
{
	int card;
	CardState from, to;
	int startTime;
	float duration;
	float delay;
	bool active;
	bool underPile;
};

enum Banner { BANNER_YOUR_TURN, BANNER_BOT_TURN, BANNER_ROUND_YOU, BANNER_ROUND_BOT, BANNER_WAR, BANNER_GAME_YOU, BANNER_GAME_BOT, BANNER_COUNT };

const char* BANNER_FILES[BANNER_COUNT] = {
	"banner_your_turn.png", "banner_bot_turn.png", "banner_round_you.png", "banner_round_bot.png",
	"banner_war.png", "banner_game_you.png", "banner_game_bot.png"
};

enum GameState { DEALING, AWAITING_CHOICE, PLAYING, REVEALING, GROWING, SHOWING, COLLECTING, GAME_OVER };

Card cards[MAX_CARDS];
bool cardExists[MAX_CARDS];
GLuint textures[MAX_CARDS];
GLuint bannerTextures[BANNER_COUNT];
int cardCount = 0;

int deck[MAX_CARDS];
int deckCount = 0;
int hand[2][MAX_CARDS];
int handCount[2] = { 0, 0 };
int tablePile[MAX_CARDS];
int tablePileCount = 0;
int topCard[2] = { -1, -1 };
int warLevel = 0;

Tween tweens[MAX_TWEENS];
int tweenCount = 0;

GameState gameState = DEALING;
int chooser = HUMAN;
int chosenStat = -1;
int roundWinner = -1;
int roundNumber = 0;
int showStartTime = 0;
int gameWinner = -1;


float RandomRange(float a, float b)
{
	return a + (b - a) * (rand() / (float)RAND_MAX);
}

float Smoothstep(float t)
{
	return t * t * (3.0f - 2.0f * t);
}

CardState MakeState(float x, float y, float angle, float scale, float theta)
{
	CardState s;
	s.x = x;
	s.y = y;
	s.angle = angle;
	s.scale = scale;
	s.theta = theta;
	return s;
}

CardState PileState(int player)
{
	return MakeState(PILE_X[player] + RandomRange(-PILE_JITTER_POS, PILE_JITTER_POS),
		PILE_Y[player] + RandomRange(-PILE_JITTER_POS, PILE_JITTER_POS),
		RandomRange(-PILE_JITTER_ANGLE, PILE_JITTER_ANGLE), PILE_SCALE, PI);
}

int StatValue(int cardId, int stat)
{
	if (stat == 0) return cards[cardId].speed;
	if (stat == 1) return cards[cardId].size;
	return cards[cardId].danger;
}

GLuint LoadTexture(const char* texturePath)
{
	GLuint texture;
	glGenTextures(1, &texture);
	glBindTexture(GL_TEXTURE_2D, texture);

	glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
	glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
	glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
	glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

	int width, height;
	unsigned char* image = SOIL_load_image(texturePath, &width, &height, 0, SOIL_LOAD_RGBA);
	if (image == NULL)
		fprintf(stderr, "Nu pot incarca textura %s\n", texturePath);
	glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, image);

	SOIL_free_image_data(image);
	glBindTexture(GL_TEXTURE_2D, 0);
	return texture;
}

void ReadCardsCsv(const char* path)
{
	FILE* f = fopen(path, "r");
	if (f == NULL)
	{
		fprintf(stderr, "Nu pot deschide %s\n", path);
		return;
	}
	char line[512];
	fgets(line, sizeof(line), f);
	while (fgets(line, sizeof(line), f) != NULL)
	{
		Card c;
		if (sscanf(line, "%d,%[^,],%d,%d,%d", &c.id, c.name, &c.speed, &c.size, &c.danger) == 5
			&& c.id >= 0 && c.id < MAX_CARDS)
		{
			c.state = MakeState(DECK_X, DECK_Y, 0.0f, PILE_SCALE, PI);
			cards[c.id] = c;
			cardExists[c.id] = true;
			cardCount++;
		}
	}
	fclose(f);
	printf("Am citit %d carti din %s\n", cardCount, path);
}

void LoadCardTextures(void)
{
	char path[64];
	for (int id = 0; id < MAX_CARDS; id++)
		if (cardExists[id])
		{
			sprintf(path, "card_%d.png", id);
			textures[id] = LoadTexture(path);
		}
}

void LoadBannerTextures(void)
{
	for (int b = 0; b < BANNER_COUNT; b++)
		bannerTextures[b] = LoadTexture(BANNER_FILES[b]);
}

void ShuffleDeck(void)
{
	deckCount = 0;
	for (int id = 0; id < MAX_CARDS; id++)
		if (cardExists[id])
			deck[deckCount++] = id;
	for (int i = deckCount - 1; i > 0; i--)
	{
		int j = rand() % (i + 1);
		int aux = deck[i];
		deck[i] = deck[j];
		deck[j] = aux;
	}
}

void AddTween(int card, CardState from, CardState to, float duration, float delay, bool underPile)
{
	if (tweenCount >= MAX_TWEENS)
		return;
	Tween& tw = tweens[tweenCount++];
	tw.card = card;
	tw.from = from;
	tw.to = to;
	tw.startTime = glutGet(GLUT_ELAPSED_TIME);
	tw.duration = duration;
	tw.delay = delay;
	tw.active = true;
	tw.underPile = underPile;
}

bool UpdateTween(Tween& tw, int now)
{
	float t = (now - tw.startTime - tw.delay) / tw.duration;
	if (t < 0.0f)
		return true;
	if (t >= 1.0f)
	{
		t = 1.0f;
		tw.active = false;
	}
	float s = Smoothstep(t);
	CardState& c = cards[tw.card].state;
	c.x = tw.from.x + (tw.to.x - tw.from.x) * s;
	c.y = tw.from.y + (tw.to.y - tw.from.y) * s;
	c.angle = tw.from.angle + (tw.to.angle - tw.from.angle) * s;
	c.scale = tw.from.scale + (tw.to.scale - tw.from.scale) * s;
	c.theta = tw.from.theta + (tw.to.theta - tw.from.theta) * s;
	return tw.active;
}

bool IsAnimating(void)
{
	return tweenCount > 0;
}

int PopTop(int player)
{
	int id = hand[player][0];
	for (int k = 1; k < handCount[player]; k++)
		hand[player][k - 1] = hand[player][k];
	handCount[player]--;
	return id;
}

void PushBottom(int player, int id)
{
	hand[player][handCount[player]++] = id;
}

void PrintPrompt(void)
{
	if (chooser == HUMAN)
		printf("Alegi tu: 1 = Speed, 2 = Size, 3 = Danger\n");
	else
		printf("Alege botul: apasa SPACE\n");
}

bool IsOnTable(int id)
{
	for (int i = 0; i < tablePileCount; i++)
		if (tablePile[i] == id)
			return true;
	return false;
}

bool IsInHand(int player, int id)
{
	for (int k = 0; k < handCount[player]; k++)
		if (hand[player][k] == id)
			return true;
	return false;
}

void DealCards(void)
{
	handCount[HUMAN] = handCount[BOT] = 0;
	tablePileCount = 0;
	topCard[HUMAN] = topCard[BOT] = -1;
	warLevel = 0;
	tweenCount = 0;
	chooser = HUMAN;
	chosenStat = -1;
	roundWinner = -1;
	roundNumber = 0;
	gameWinner = -1;

	ShuffleDeck();
	int total = 2 * CARDS_PER_PLAYER;
	if (total > deckCount)
		total = deckCount;

	for (int i = 0; i < total; i++)
	{
		int id = deck[i];
		cards[id].state = MakeState(DECK_X + RandomRange(-PILE_JITTER_POS, PILE_JITTER_POS),
			DECK_Y + RandomRange(-PILE_JITTER_POS, PILE_JITTER_POS),
			RandomRange(-PILE_JITTER_ANGLE, PILE_JITTER_ANGLE), PILE_SCALE, PI);
	}
	for (int i = 0; i < total; i++)
	{
		int id = deck[i];
		int player = i % 2;
		PushBottom(player, id);
		AddTween(id, cards[id].state, PileState(player), MOVE_DURATION, i * DEAL_GAP, true);
	}
	deckCount = 0;
	gameState = DEALING;
	printf("\n=== Joc nou: %d carti fiecare ===\n", CARDS_PER_PLAYER);
}

void EndGame(int winner)
{
	gameWinner = winner;
	chosenStat = -1;
	roundWinner = -1;
	gameState = GAME_OVER;
	if (winner == -1)
		printf("=== Joc terminat: egalitate, nimeni nu mai are carti ===\n");
	else
		printf("=== Joc terminat: castiga %s cu toate cele %d carti ===\n", PLAYER_NAMES[winner], handCount[winner]);
	printf("Apasa R pentru joc nou\n");
}

void PlaceCardsOnTable(void)
{
	for (int p = HUMAN; p <= BOT; p++)
	{
		topCard[p] = -1;
		if (handCount[p] == 0)
			continue;
		int id = PopTop(p);
		topCard[p] = id;
		tablePile[tablePileCount++] = id;
		float side = (p == HUMAN) ? -1.0f : 1.0f;
		float angle = (warLevel == 0) ? 0.0f : RandomRange(-WAR_JITTER_ANGLE, WAR_JITTER_ANGLE);
		CardState target = MakeState(TABLE_X[p] + side * warLevel * WAR_OFFSET_X,
			TABLE_Y[p] + warLevel * WAR_OFFSET_Y, angle, PILE_SCALE, PI);
		AddTween(id, cards[id].state, target, MOVE_DURATION, 0.0f, false);
	}
	gameState = PLAYING;
}

void PlayRound(int stat)
{
	chosenStat = stat;
	roundNumber++;
	warLevel = 0;
	tablePileCount = 0;
	PlaceCardsOnTable();
}

void PlayWar(void)
{
	warLevel++;
	if (handCount[HUMAN] == 0 && handCount[BOT] == 0)
	{
		EndGame(-1);
		return;
	}
	PlaceCardsOnTable();
}

void RevealCards(void)
{
	for (int p = HUMAN; p <= BOT; p++)
	{
		int id = topCard[p];
		if (id == -1)
			continue;
		CardState faceUp = cards[id].state;
		faceUp.theta = 0.0f;
		AddTween(id, cards[id].state, faceUp, FLIP_DURATION, 0.0f, false);
	}
	gameState = REVEALING;
}

void GrowCards(void)
{
	for (int p = HUMAN; p <= BOT; p++)
	{
		int id = topCard[p];
		if (id == -1)
			continue;
		CardState grown = cards[id].state;
		grown.scale = TABLE_SCALE;
		AddTween(id, cards[id].state, grown, SCALE_DURATION, 0.0f, false);
	}
	gameState = GROWING;
}

void PrintPlayedCard(int player)
{
	if (topCard[player] == -1)
		printf("%s: fara carti", PLAYER_NAMES[player]);
	else
		printf("%s: %s (%d)", PLAYER_NAMES[player], cards[topCard[player]].name, StatValue(topCard[player], chosenStat));
}

void ResolveRound(void)
{
	if (topCard[HUMAN] == -1)
		roundWinner = BOT;
	else if (topCard[BOT] == -1)
		roundWinner = HUMAN;
	else
	{
		int humanValue = StatValue(topCard[HUMAN], chosenStat);
		int botValue = StatValue(topCard[BOT], chosenStat);
		if (humanValue > botValue)
			roundWinner = HUMAN;
		else if (botValue > humanValue)
			roundWinner = BOT;
		else
			roundWinner = -1;
	}

	if (warLevel == 0)
		printf("Runda %d: a ales %s, stat %s | ", roundNumber, PLAYER_NAMES[chooser], STAT_NAMES[chosenStat]);
	else
		printf("  Razboi %d, acelasi stat %s | ", warLevel, STAT_NAMES[chosenStat]);
	PrintPlayedCard(HUMAN);
	printf(" vs ");
	PrintPlayedCard(BOT);
	if (roundWinner == -1)
		printf(" | egalitate: razboi! pe masa sunt %d carti\n", tablePileCount);
	else
		printf(" | castiga %s si ia %d carti | carti: Tu %d, Botul %d\n", PLAYER_NAMES[roundWinner], tablePileCount,
			handCount[HUMAN] + (roundWinner == HUMAN ? tablePileCount : 0),
			handCount[BOT] + (roundWinner == BOT ? tablePileCount : 0));

	showStartTime = glutGet(GLUT_ELAPSED_TIME);
	gameState = SHOWING;
}

void CollectCards(void)
{
	for (int i = tablePileCount - 1; i >= 0; i--)
	{
		int id = tablePile[i];
		CardState onTable = cards[id].state;
		CardState shrunk = onTable;
		shrunk.scale = PILE_SCALE;
		CardState faceDown = shrunk;
		faceDown.theta = PI;
		AddTween(id, onTable, shrunk, SCALE_DURATION, 0.0f, true);
		AddTween(id, shrunk, faceDown, FLIP_DURATION, SCALE_DURATION, true);
		AddTween(id, faceDown, PileState(roundWinner), MOVE_DURATION, SCALE_DURATION + FLIP_DURATION + i * DEAL_GAP, true);
	}
	for (int i = 0; i < tablePileCount; i++)
		PushBottom(roundWinner, tablePile[i]);
	gameState = COLLECTING;
}

void FinishCollecting(void)
{
	tablePileCount = 0;
	topCard[HUMAN] = topCard[BOT] = -1;
	warLevel = 0;
	chosenStat = -1;
	chooser = roundWinner;
	roundWinner = -1;

	if (handCount[HUMAN] == 0 || handCount[BOT] == 0)
	{
		EndGame(handCount[HUMAN] > 0 ? HUMAN : BOT);
		return;
	}
	gameState = AWAITING_CHOICE;
	PrintPrompt();
}

void OnAnimationsFinished(void)
{
	switch (gameState)
	{
	case DEALING:
		gameState = AWAITING_CHOICE;
		PrintPrompt();
		break;
	case PLAYING:
		RevealCards();
		break;
	case REVEALING:
		GrowCards();
		break;
	case GROWING:
		ResolveRound();
		break;
	case COLLECTING:
		FinishCollecting();
		break;
	default:
		break;
	}
}

void UpdateAnimations(void)
{
	int now = glutGet(GLUT_ELAPSED_TIME);
	if (IsAnimating())
	{
		bool anyActive = false;
		for (int i = 0; i < tweenCount; i++)
			if (tweens[i].active && UpdateTween(tweens[i], now))
				anyActive = true;
		if (!anyActive)
		{
			tweenCount = 0;
			OnAnimationsFinished();
		}
		glutPostRedisplay();
	}
	else if (gameState == SHOWING && now - showStartTime >= SHOW_DURATION)
	{
		if (roundWinner == -1)
			PlayWar();
		else
			CollectCards();
		glutPostRedisplay();
	}
}

void CreateVBO(void)
{
	// varfurile
	GLfloat Vertices[] = {
		-50.0f, -70.0f, 0.0f, 1.0f,   0.0f, 0.0f,
		-50.0f,  70.0f, 0.0f, 1.0f,   0.0f, 1.0f,
		 50.0f, -70.0f, 0.0f, 1.0f,   1.0f, 0.0f,
		 50.0f,  70.0f, 0.0f, 1.0f,   1.0f, 1.0f
	};

	GLuint Indices[] = {
		0, 2, 3,
		0, 3, 1
	};

	// se creeaza un buffer nou
	glGenBuffers(1, &VboId);
	// este setat ca buffer curent
	glBindBuffer(GL_ARRAY_BUFFER, VboId);
	// varfurile sunt "copiate" in bufferul curent
	glBufferData(GL_ARRAY_BUFFER, sizeof(Vertices), Vertices, GL_STATIC_DRAW);

	// se creeaza / se leaga un VAO (Vertex Array Object) - util cand se utilizeaza mai multe VBO
	glGenVertexArrays(1, &VaoId);
	glBindVertexArray(VaoId);

	glGenBuffers(1, &EboId);
	glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, EboId);
	glBufferData(GL_ELEMENT_ARRAY_BUFFER, sizeof(Indices), Indices, GL_STATIC_DRAW);

	// se activeaza lucrul cu atribute; atributul 0 = pozitie
	glEnableVertexAttribArray(0);
	//
	glVertexAttribPointer(0, 4, GL_FLOAT, GL_FALSE, 6 * sizeof(GLfloat), (GLvoid*)0);

	glEnableVertexAttribArray(2);
	glVertexAttribPointer(2, 2, GL_FLOAT, GL_FALSE, 6 * sizeof(GLfloat), (GLvoid*)(4 * sizeof(GLfloat)));
}
void DestroyVBO(void)
{
	glDisableVertexAttribArray(2);
	glDisableVertexAttribArray(0);

	glBindBuffer(GL_ARRAY_BUFFER, 0);
	glDeleteBuffers(1, &EboId);
	glDeleteBuffers(1, &VboId);

	glBindVertexArray(0);
	glDeleteVertexArrays(1, &VaoId);
}

void CreateBackgroundVBO(void)
{
	GLfloat Vertices[] = {
		-1.0f, -1.0f, 0.0f, 1.0f,   BG_BOTTOM[0], BG_BOTTOM[1], BG_BOTTOM[2], 1.0f,
		 1.0f, -1.0f, 0.0f, 1.0f,   BG_BOTTOM[0], BG_BOTTOM[1], BG_BOTTOM[2], 1.0f,
		-1.0f,  0.0f, 0.0f, 1.0f,   BG_MIDDLE[0], BG_MIDDLE[1], BG_MIDDLE[2], 1.0f,
		 1.0f,  0.0f, 0.0f, 1.0f,   BG_MIDDLE[0], BG_MIDDLE[1], BG_MIDDLE[2], 1.0f,
		-1.0f,  1.0f, 0.0f, 1.0f,   BG_TOP[0], BG_TOP[1], BG_TOP[2], 1.0f,
		 1.0f,  1.0f, 0.0f, 1.0f,   BG_TOP[0], BG_TOP[1], BG_TOP[2], 1.0f
	};

	glGenVertexArrays(1, &BackgroundVaoId);
	glBindVertexArray(BackgroundVaoId);

	glGenBuffers(1, &BackgroundVboId);
	glBindBuffer(GL_ARRAY_BUFFER, BackgroundVboId);
	glBufferData(GL_ARRAY_BUFFER, sizeof(Vertices), Vertices, GL_STATIC_DRAW);

	glEnableVertexAttribArray(0);
	glVertexAttribPointer(0, 4, GL_FLOAT, GL_FALSE, 8 * sizeof(GLfloat), (GLvoid*)0);

	glEnableVertexAttribArray(1);
	glVertexAttribPointer(1, 4, GL_FLOAT, GL_FALSE, 8 * sizeof(GLfloat), (GLvoid*)(4 * sizeof(GLfloat)));
}
void DestroyBackgroundVBO(void)
{
	glBindVertexArray(BackgroundVaoId);
	glDisableVertexAttribArray(1);
	glDisableVertexAttribArray(0);

	glBindBuffer(GL_ARRAY_BUFFER, 0);
	glDeleteBuffers(1, &BackgroundVboId);

	glBindVertexArray(0);
	glDeleteVertexArrays(1, &BackgroundVaoId);
}

void CreateBannerVBO(void)
{
	float w = BANNER_WIDTH / 2.0f, h = BANNER_HEIGHT / 2.0f;
	GLfloat Vertices[] = {
		-w, -h, 0.0f, 1.0f,   0.0f, 0.0f,
		-w,  h, 0.0f, 1.0f,   0.0f, 1.0f,
		 w, -h, 0.0f, 1.0f,   1.0f, 0.0f,
		 w,  h, 0.0f, 1.0f,   1.0f, 1.0f
	};

	GLuint Indices[] = {
		0, 2, 3,
		0, 3, 1
	};

	glGenVertexArrays(1, &BannerVaoId);
	glBindVertexArray(BannerVaoId);

	glGenBuffers(1, &BannerVboId);
	glBindBuffer(GL_ARRAY_BUFFER, BannerVboId);
	glBufferData(GL_ARRAY_BUFFER, sizeof(Vertices), Vertices, GL_STATIC_DRAW);

	glGenBuffers(1, &BannerEboId);
	glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, BannerEboId);
	glBufferData(GL_ELEMENT_ARRAY_BUFFER, sizeof(Indices), Indices, GL_STATIC_DRAW);

	glEnableVertexAttribArray(0);
	glVertexAttribPointer(0, 4, GL_FLOAT, GL_FALSE, 6 * sizeof(GLfloat), (GLvoid*)0);

	glEnableVertexAttribArray(2);
	glVertexAttribPointer(2, 2, GL_FLOAT, GL_FALSE, 6 * sizeof(GLfloat), (GLvoid*)(4 * sizeof(GLfloat)));
}
void DestroyBannerVBO(void)
{
	glBindVertexArray(BannerVaoId);
	glDisableVertexAttribArray(2);
	glDisableVertexAttribArray(0);

	glBindBuffer(GL_ARRAY_BUFFER, 0);
	glDeleteBuffers(1, &BannerEboId);
	glDeleteBuffers(1, &BannerVboId);

	glBindVertexArray(0);
	glDeleteVertexArrays(1, &BannerVaoId);
}

void CreateShaders(void)
{
	BackgroundProgramId = LoadShaders("example.vert", "example.frag");
	ProgramId = LoadShaders("card.vert", "card.frag");
	glUseProgram(ProgramId);
}
void DestroyShaders(void)
{
	glDeleteProgram(ProgramId);
	glDeleteProgram(BackgroundProgramId);
}

void Initialize(void)
{
	glClearColor(BG_MIDDLE[0], BG_MIDDLE[1], BG_MIDDLE[2], 1.0f); // culoarea de fond a ecranului
	srand((unsigned int)time(NULL));
	CreateVBO();
	CreateBackgroundVBO();
	CreateBannerVBO();
	ReadCardsCsv("fish_seed.csv");
	LoadCardTextures();
	LoadBannerTextures();
	CreateShaders();
	myMatrixLocation = glGetUniformLocation(ProgramId, "myMatrix");
	textureLocation = glGetUniformLocation(ProgramId, "myTexture");
	highlightRowLocation = glGetUniformLocation(ProgramId, "highlightRow");
	winnerLocation = glGetUniformLocation(ProgramId, "isWinner");
	resizeMatrix = glm::ortho(-400.0f, 400.0f, -400.0f, 400.0f);
	DealCards();
}

void DrawCard(int id)
{
	CardState& s = cards[id].state;
	translateMatrix = glm::translate(glm::mat4(1.0f), glm::vec3(s.x, s.y, 0.0f));
	rotateMatrix = glm::rotate(glm::mat4(1.0f), s.angle, glm::vec3(0.0f, 0.0f, 1.0f));
	scaleMatrix = glm::scale(glm::mat4(1.0f), glm::vec3(s.scale, s.scale, 1.0f));
	flipMatrix = glm::scale(glm::mat4(1.0f), glm::vec3(cos(s.theta), 1.0f, 1.0f));
	myMatrix = resizeMatrix * translateMatrix * rotateMatrix * scaleMatrix * flipMatrix;
	glUniformMatrix4fv(myMatrixLocation, 1, GL_FALSE, &myMatrix[0][0]);

	bool onTable = IsOnTable(id);
	bool roundWinnerCard = gameState == SHOWING && roundWinner != -1 && id == topCard[roundWinner];
	bool gameWinnerCard = gameState == GAME_OVER && gameWinner != -1 && IsInHand(gameWinner, id);
	glUniform1i(highlightRowLocation, onTable ? chosenStat : -1);
	glUniform1i(winnerLocation, roundWinnerCard || gameWinnerCard);

	glBindTexture(GL_TEXTURE_2D, textures[id]);
	glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_INT, 0);
}

void DrawMovingCards(bool underPile, bool drawn[])
{
	for (int i = tweenCount - 1; i >= 0; i--)
		if (tweens[i].active && tweens[i].underPile == underPile && !drawn[tweens[i].card])
		{
			drawn[tweens[i].card] = true;
			DrawCard(tweens[i].card);
		}
}

void DrawScene(void)
{
	bool moving[MAX_CARDS] = { false };
	bool drawn[MAX_CARDS] = { false };
	for (int i = 0; i < tweenCount; i++)
		if (tweens[i].active)
			moving[tweens[i].card] = true;

	DrawMovingCards(true, drawn);

	for (int p = HUMAN; p <= BOT; p++)
		for (int k = handCount[p] - 1; k >= 0; k--)
			if (!moving[hand[p][k]])
				DrawCard(hand[p][k]);

	if (gameState != COLLECTING)
		for (int i = 0; i < tablePileCount; i++)
			if (!moving[tablePile[i]])
				DrawCard(tablePile[i]);

	DrawMovingCards(false, drawn);
}

int CurrentBanner(void)
{
	if (gameState == AWAITING_CHOICE && !IsAnimating())
		return chooser == HUMAN ? BANNER_YOUR_TURN : BANNER_BOT_TURN;
	if (gameState == SHOWING)
	{
		if (roundWinner == -1)
			return BANNER_WAR;
		return roundWinner == HUMAN ? BANNER_ROUND_YOU : BANNER_ROUND_BOT;
	}
	if (gameState == GAME_OVER && gameWinner != -1)
		return gameWinner == HUMAN ? BANNER_GAME_YOU : BANNER_GAME_BOT;
	return -1;
}

void DrawBanner(void)
{
	int banner = CurrentBanner();
	if (banner == -1)
		return;
	translateMatrix = glm::translate(glm::mat4(1.0f), glm::vec3(0.0f, BANNER_Y, 0.0f));
	myMatrix = resizeMatrix * translateMatrix;
	glUniformMatrix4fv(myMatrixLocation, 1, GL_FALSE, &myMatrix[0][0]);
	glUniform1i(highlightRowLocation, -1);
	glUniform1i(winnerLocation, 0);

	glBindVertexArray(BannerVaoId);
	glBindTexture(GL_TEXTURE_2D, bannerTextures[banner]);
	glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_INT, 0);
}

void DrawBackground(void)
{
	glUseProgram(BackgroundProgramId);
	glBindVertexArray(BackgroundVaoId);
	glDrawArrays(GL_TRIANGLE_STRIP, 0, 6);
	glUseProgram(ProgramId);
}

void RenderFunction(void)
{
	glClear(GL_COLOR_BUFFER_BIT);

	glActiveTexture(GL_TEXTURE0);
	glUniform1i(textureLocation, 0);

	// Functiile de desenare
	DrawBackground();
	glBindVertexArray(VaoId);
	DrawScene();
	DrawBanner();

	glutSwapBuffers();
	glFlush();
}
void Cleanup(void)
{
	DestroyShaders();
	DestroyBannerVBO();
	DestroyBackgroundVBO();
	DestroyVBO();
	for (int id = 0; id < MAX_CARDS; id++)
		if (cardExists[id])
			glDeleteTextures(1, &textures[id]);
	glDeleteTextures(BANNER_COUNT, bannerTextures);
}

void ProcessNormalKeys(unsigned char key, int x, int y)
{
	if (IsAnimating())
		return;
	if (key == 'r' || key == 'R')
	{
		DealCards();
		return;
	}
	if (gameState != AWAITING_CHOICE)
		return;
	if (chooser == HUMAN && key >= '1' && key <= '3')
		PlayRound(key - '1');
	else if (chooser == BOT && key == ' ')
		PlayRound(rand() % 3);
}

int main(int argc, char* argv[])
{
	glutInit(&argc, argv);
	glutInitDisplayMode(GLUT_DOUBLE | GLUT_RGB);
	glutInitWindowPosition(100, 100); // pozitia initiala a ferestrei
	glutInitWindowSize(600, 600); //dimensiunile ferestrei
	glutCreateWindow("Comorile Oceanelor"); // titlul ferestrei
	glewInit(); // nu uitati de initializare glew; trebuie initializat inainte de a a initializa desenarea
	Initialize();
	glutDisplayFunc(RenderFunction);
	glutIdleFunc(UpdateAnimations);
	glutKeyboardFunc(ProcessNormalKeys);
	glutCloseFunc(Cleanup);
	glutMainLoop();
}

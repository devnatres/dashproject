package com.devnatres.dashproject.levelsystem;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.devnatres.dashproject.DashGame;
import com.devnatres.dashproject.Debug;
import com.devnatres.dashproject.DnaAnimation;
import com.devnatres.dashproject.DnaCamera;
import com.devnatres.dashproject.agents.*;
import com.devnatres.dashproject.gameconstants.EAnimations;
import com.devnatres.dashproject.gameconstants.Parameters;
import com.devnatres.dashproject.gameconstants.Time;
import com.devnatres.dashproject.gameinput.InputTranslator;
import com.devnatres.dashproject.levelscriptcmd.LevelScript;
import com.devnatres.dashproject.sidescreens.MainMenuScreen;
import com.devnatres.dashproject.space.DirectionSelector;
import com.devnatres.dashproject.store.HyperStore;
import com.devnatres.dashproject.tools.Tools;

import static com.devnatres.dashproject.agents.AgentRegistry.EAgentLayer;

/**
 * Created by DevNatres on 04/12/2014.
 */
public class LevelScreen implements Screen {
    private static final int RESET_COUNT = 180;
    private static final int SCORE_HORDE_COMBO_DURATION = 90;

    private int resetCountDown = RESET_COUNT;

    private final DashGame dashGame;
    private final SpriteBatch mainBatch;
    private final ShapeRenderer mainShape;
    private final BitmapFont mainFont;
    private final DnaCamera mainCamera;

    private final DnaCamera fixedCamera;

    private final HyperStore hyperStore;

    private final LevelMap map;

    private final Hero hero;

    private final Music badassMusic;

    private final InputTranslator inputTranslator;

    private final Texture backgroundTexture;
    private final Texture grayScreenTexture;

    private final float screenWidth;
    private final float screenHeight;

    private float bulletTime;

    private final AgentRegistry agentRegistry;
    private final HordeGroup hordeGroup;

    private final LevelScript levelScript;
    private final Texture dissipatedMessage;

    private int totalScore;
    private int lastTotalScore;
    private String scoreString = "0";
    private String scoreHordeComboString;
    private int scoreHordeComboDuration;

    private boolean comboCameraChasing;

    private final DnaAnimation radar;

    private boolean skipCameraAssistant;

    public LevelScreen(DashGame game, String levelName) {
        this.dashGame = game;

        screenWidth = game.getScreenWidth();
        screenHeight = game.getScreenHeight();
        mainBatch = game.getMainBatch();
        mainShape = game.getMainShape();
        mainFont = game.getMainFont();
        mainCamera = game.getMainCamera();

        hyperStore = game.getHyperStore();

        mainCamera.setToOrtho(false, game.getScreenWidth(), game.getScreenHeight());

        fixedCamera = new DnaCamera();
        fixedCamera.setToOrtho(false, Parameters.INITIAL_SCREEN_WIDTH, Parameters.INITIAL_SCREEN_HEIGHT);

        map = new LevelMap(levelName);

        agentRegistry = new AgentRegistry();

        hero = new Hero(hyperStore, this);
        agentRegistry.register(hero, EAgentLayer.TRUNK);
        hordeGroup = new HordeGroup(this);

        levelScript = new LevelScript();

        extractScript();

        badassMusic = hyperStore.getMusic("music/badass.ogg");
        badassMusic.setLooping(true);

        inputTranslator = new InputTranslator();

        mainShape.setColor(Color.WHITE);

        backgroundTexture = hyperStore.getTexture("background.jpg");
        grayScreenTexture = hyperStore.getTexture("gray_screen.png");
        dissipatedMessage = hyperStore.getTexture("dissipated.png");

        radar = EAnimations.RADAR_INDICATOR.create(hyperStore);

        System.gc();
        inputTranslator.clear();
    }

    public Hero getHero() {
        return hero;
    }

    public DnaCamera getCamera() {
        return mainCamera;
    }

    public LevelScript getLevelScript() {
        return levelScript;
    }

    private void extractScript() {
        map.extractLevelScript(hyperStore, this);
    }

    public void addHorde(Horde horde) {
        agentRegistry.register(horde, EAgentLayer.FLOOR);
        hordeGroup.removeKilledHordes();
        hordeGroup.addLinked(horde);
    }

    @Override
    public void render(float delta) {
        if (inputTranslator.isResetRequested()) {
            reset();
            return;
        }

        clearScreen();

        levelScript.execute();

        if (skipCameraAssistant || bulletTime == 0 || comboCameraChasing) {
            chaseHeroWithCamera();
        } else { // We can't still decide not to chase, we must check if there aren't foes on hero's visible scope
            if (isAllFoesOutOfVisibleScope()) {
                comboCameraChasing = true;
                chaseHeroWithCamera();
                if (comboCameraChasing) { // Can be modified in the method above
                    activateBulletTime();
                }
            }
        }

        mainCamera.update();

        //  TODO Optimize by sharing the same begin-end batch sequence.
        // A)
        // pass the mainBatch to the map,
        // and override OrthogonalTiledMapRenderer's (BatchTiledMapRenderer, actually)
        // beginRender() and endRender() methods.
        // B)
        // renderer.getSpriteBatch().begin()
        // renderer.rendererTileLayer((TiledMapTileLayer) map.getLayers().get("background"));
        // player.draw(renderer.getSpriteBatch());
        // renderer.rendererTileLayer((TiledMapTileLayer) map.getLayers().get("foreground"));
        // renderer.getSpriteBatch().end()
        mainBatch.setProjectionMatrix(mainCamera.combined);
        renderBackground();
        renderMap();
        renderSprites();
        renderFoeRadar();
        updateTotalScore();
        renderHub();
        renderMessages();

        if (Debug.DEBUG) {
            renderDebugger();
        }

        if (resetCountDown == 0) {
            reset();
        }

    }

    private boolean isAllFoesOutOfVisibleScope() {
        boolean allFoesOutOfVisibleScope = true;
        Horde globalHorde = getGlobalHorde();
        for (int i = 0; i < globalHorde.size(); i++){
            Foe foe = globalHorde.getFoe(i);
            if (mainCamera.isOnCamera(foe) && !foe.isDying() && hero.isFoeOnScope(foe)) {
                allFoesOutOfVisibleScope = false;
                break;
            }
        }
        return allFoesOutOfVisibleScope;
    }

    private void reset() {
        dashGame.setScreen(new MainMenuScreen(dashGame));
    }

    private void chaseHeroWithCamera() {
        moveCameraTo(hero.getAuxCenter().x, hero.getAuxCenter().y, Time.FAST_CAMERA_SPEED);
    }

    @Override
    public void resize(int width, int height) {
        /*mainCamera.setToOrtho(false, width, height);
        mainCamera.update();*/
    }

    public LevelMap getMap() {
        return map;
    }

    public Horde getGlobalHorde() {
        return hordeGroup.getGlobalHorde();
    }

    private void moveCameraTo(float posX, float posY, float speed) {
        posX = limitCameraTargetX(posX);
        posY = limitCameraTargetY(posY);

        if (posX != mainCamera.position.x || posY != mainCamera.position.y) {
            Vector3 vTarget = new Vector3(posX, posY, 0);
            if (speed == 0 || Math.abs(vTarget.dst(mainCamera.position)) <= speed) {
                mainCamera.position.set(vTarget);
                comboCameraChasing = false;
            } else {
                Vector3 vJump = vTarget.sub(mainCamera.position).nor().scl(speed);
                // Slow movement needs to be rounded to avoid seeing tile map flickering
                if (speed < Time.MEDIUM_CAMERA_SPEED) {
                    mainCamera.translate(Math.round(vJump.x), Math.round(vJump.y));
                } else {
                    mainCamera.translate(vJump.x, vJump.y);
                }
            }
        } else {
            comboCameraChasing = false;
        }
    }

    public void updateTotalScore() {
        if (lastTotalScore != totalScore) {
            lastTotalScore = totalScore;
            scoreString = String.valueOf(totalScore);
        }
    }

    public void processFoeDamageResult(FoeDamageResult foeDamageResult) {
        totalScore += foeDamageResult.getScore();
    }

    public void processHordeDamageResult(HordeDamageResult hordeDamageResult) {
        int hordeComboScore = hordeDamageResult.getComboScore();
        totalScore += hordeComboScore;
        scoreHordeComboString = String.valueOf(hordeComboScore);
        scoreHordeComboDuration = SCORE_HORDE_COMBO_DURATION;
    }

    private float limitCameraTargetX(float x) {
        final float minX = screenWidth/2;
        final float maxX = map.getMapPixelWidth() - screenWidth/2;
        return Math.round(Tools.limit_f(x, minX, maxX));
    }

    private float limitCameraTargetY(float y) {
        final float minY = screenHeight/2;
        final float maxY = map.getMapPixelHeight() - screenHeight/2;
        return Math.round(Tools.limit_f(y, minY, maxY));
    }

    private void clearScreen() {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        //Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
    }

    private void renderBackground() {
        mainBatch.begin();
        mainBatch.draw(backgroundTexture, 300, 0);
        mainBatch.end();
    }

    private void renderMap() {
        map.paint(mainCamera);
    }

    private void renderHub() {
        mainBatch.setProjectionMatrix(fixedCamera.combined);
        mainBatch.begin();
        mainFont.draw(mainBatch, scoreString, 50, screenHeight - 10);
        if (scoreHordeComboDuration > 0) {
            mainFont.draw(mainBatch, scoreHordeComboString, screenWidth/2, screenHeight - 100);
            scoreHordeComboDuration--;
        }
        mainBatch.end();
    }

    private void renderMessages() {
        if (!hero.isVisible()) {
            mainBatch.setProjectionMatrix(fixedCamera.combined);
            mainBatch.begin();
            mainBatch.draw(dissipatedMessage,
                    (screenWidth - dissipatedMessage.getWidth())/2,
                    (screenHeight - dissipatedMessage.getHeight())/2);
            mainBatch.end();
            if (resetCountDown > 0) {
                resetCountDown--;
            }
        }
    }

    private void renderSprites() {
        inputForHero();

        mainBatch.begin();

        agentRegistry.render(Time.FRAME, mainBatch);

        renderSprites_Cover();

        mainBatch.end();

        if (bulletTime > 0f && !comboCameraChasing) {
            bulletTime--;
        }
    }

    private void renderSprites_Cover() {
        if (hero.isCoverLeft()) {
            mainBatch.draw(grayScreenTexture,
                    hero.getX() - grayScreenTexture.getWidth(),
                    mainCamera.position.y - grayScreenTexture.getHeight()/2);
        }
        if (hero.isCoverUp()) {
            mainBatch.draw(grayScreenTexture,
                    mainCamera.position.x - grayScreenTexture.getWidth()/2,
                    hero.getY() + hero.getHeight());
        }
        if (hero.isCoverRight()) {
            mainBatch.draw(grayScreenTexture,
                    hero.getX() + hero.getWidth(),
                    mainCamera.position.y - grayScreenTexture.getHeight()/2);
        }
        if (hero.isCoverDown()) {
            mainBatch.draw(grayScreenTexture,
                    mainCamera.position.x - grayScreenTexture.getWidth()/2,
                    hero.getY() - grayScreenTexture.getHeight());
        }
    }

    private void renderFoeRadar() {
        Horde globalHorde = hordeGroup.getGlobalHorde();
        boolean thereIsFoeOnCamera = false;
        for (int i = 0; i < globalHorde.size(); i++){
            Foe foe = globalHorde.getFoe(i);
            if (mainCamera.isOnCamera(foe) && !foe.isDying()) {
                thereIsFoeOnCamera = true;
                break;
            }
        }

        if (!thereIsFoeOnCamera) {
            mainBatch.begin();
            for (int i = 0; i < hordeGroup.size(); i++){
                Horde horde = hordeGroup.getHorde(i);
                if (!horde.isKilled()) {
                    radar.updateCurrentStateTime();
                    renderFoeRadar_indicator(horde.getReferencePosition());
                }
            }
            mainBatch.end();
        }
    }


    private void renderFoeRadar_indicator(Vector2 referencePos) {
        DirectionSelector directionSelector = mainCamera.getOutDirection(referencePos.x, referencePos.y);
        TextureRegion textureRegion = radar.getCurrentKeyFrame();
        float spriteLeft, spriteBottom;

        if (directionSelector.isUp()) {
            spriteBottom = mainCamera.getUp() - textureRegion.getRegionHeight();
            if (directionSelector.isLeft()) {
                spriteLeft = mainCamera.getLeft();
            } else if (directionSelector.isRight()) {
                spriteLeft = mainCamera.getRight() - textureRegion.getRegionWidth();
            } else {
                spriteLeft = referencePos.x - textureRegion.getRegionWidth()/2f;
                if (spriteLeft < mainCamera.getLeft()) {
                    spriteLeft = mainCamera.getLeft();
                } else if ((spriteLeft+textureRegion.getRegionWidth()) > mainCamera.getRight()) {
                    spriteLeft = mainCamera.getRight() - textureRegion.getRegionWidth();
                }
            }
        } else if (directionSelector.isDown()) {
            spriteBottom = mainCamera.getDown();
            if (directionSelector.isLeft()) {
                spriteLeft = mainCamera.getLeft();
            } else if (directionSelector.isRight()) {
                spriteLeft = mainCamera.getRight() - textureRegion.getRegionWidth();
            } else {
                spriteLeft = referencePos.x - textureRegion.getRegionWidth() / 2f;
                if (spriteLeft < mainCamera.getLeft()) {
                    spriteLeft = mainCamera.getLeft();
                } else if ((spriteLeft+textureRegion.getRegionWidth()) > mainCamera.getRight()) {
                    spriteLeft = mainCamera.getRight() - textureRegion.getRegionWidth();
                }
            }
        } else {
            if (directionSelector.isLeft()) {
                spriteLeft = mainCamera.getLeft();
                spriteBottom = referencePos.y - textureRegion.getRegionHeight()/2f;
            } else { // isRight()
                spriteLeft = mainCamera.getRight() - textureRegion.getRegionWidth();
                spriteBottom = referencePos.y - textureRegion.getRegionHeight()/2f;
            }

            if (spriteBottom < mainCamera.getDown()) {
                spriteBottom = mainCamera.getDown();
            } else if ((spriteBottom+textureRegion.getRegionHeight()) > mainCamera.getUp()) {
                spriteBottom = mainCamera.getUp() - textureRegion.getRegionHeight();
            }
        }

        mainBatch.draw(textureRegion, spriteLeft, spriteBottom);
    }

    private void inputForHero() {

        Vector2 touchDownPoint = inputTranslator.getTouchDownPoint();

        if (touchDownPoint != null) {
            Vector3 clickCoordinates = new Vector3(touchDownPoint.x, touchDownPoint.y, 0);
            Vector3 position = mainCamera.unproject(clickCoordinates);
            hero.programNextPos(position.x, position.y);
        }

        //hero.act(delta);
    }

    public void activateBulletTime() {
        bulletTime = Time.BULLET_TIME;
    }

    public void deactivateBulletTime() {
        bulletTime = 0;
    }

    public boolean isBulletTime() {
        return bulletTime > 0;
    }

    public void register(Agent agent, EAgentLayer layer) {
        agentRegistry.register(agent, layer);
    }

    private void renderDebugger() {

        mainShape.setProjectionMatrix(mainCamera.combined);
        Horde globalHorde = getGlobalHorde();
        for (int i = 0, n = globalHorde.size(); i < n; i++) {
            Foe foe = globalHorde.getFoe(i);

            Vector2 foePosition = foe.getAuxCenter();
            boolean hidden = false;
            if (foePosition.x < hero.getX() && hero.isCoverLeft()) {
                hidden = true;
            }
            if (foePosition.x > hero.getX() && hero.isCoverRight()) {
                hidden = true;
            }
            if (foePosition.y < hero.getY() && hero.isCoverDown()) {
                hidden = true;
            }
            if (foePosition.y > hero.getY() && hero.isCoverUp()) {
                hidden = true;
            }

            if (!hidden && mainCamera.isOnCamera(foe)) {
                mainShape.begin(ShapeRenderer.ShapeType.Line);
                mainShape.line(foePosition.x, foePosition.y,
                        hero.getX() + hero.getWidth() / 2, hero.getY() + hero.getHeight() / 2);
                mainShape.end();
            }
        }
    }

    @Override
    public void show() {
        //badassMusic.play();
    }

    @Override
    public void hide() {
        dispose();
    }

    @Override
    public void pause() {

    }

    @Override
    public void resume() {

    }

    @Override
    public void dispose() {
        map.dispose();
    }


}

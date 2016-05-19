package app.drewromanyk.com.minesweeper.models;

/**
 * Created by Drew on 12/7/2014.
 */

import android.content.Context;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.GridLayout;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.games.Games;

import java.util.Timer;
import java.util.TimerTask;

import app.drewromanyk.com.minesweeper.BuildConfig;
import app.drewromanyk.com.minesweeper.R;
import app.drewromanyk.com.minesweeper.activities.GameActivity;
import app.drewromanyk.com.minesweeper.enums.GameDifficulty;
import app.drewromanyk.com.minesweeper.enums.GameSoundType;
import app.drewromanyk.com.minesweeper.enums.GameStatus;
import app.drewromanyk.com.minesweeper.util.UserPrefStorage;


public class Board {
    private String TAG = "Board";

    private GameActivity gameActivity;

    private Cell[][] cell;
    private CellNeighbors[][] cellNeighbors;
    private int columns;
    private int rows;
    private GameStatus gameStatus;
    private GameDifficulty gameDifficulty;
    private int mineCount;
    private int flaggedMines;
    private int flaggedCells;
    private int cellsInGame;
    private int revealedCells;
    private boolean firstRound;
    private GridLayout board;
    private boolean tappedOnRevealedCell = false;
    private ThreeBV score3BV;
    private long gameTime;
    private boolean isGameTimerOn = false;
    private Timer timer;

    /*
     * SETUP BOARD
     */
    //columns and rows to set how many there will be
    //mineCount to set the amount of bombs for the game
    public Board(int rows, int columns, int mineCount, GameDifficulty gameDifficulty, GameActivity gameActivity) {
        this.columns = columns;
        this.rows = rows;
        this.mineCount = mineCount;
        this.gameDifficulty = gameDifficulty;
        this.gameActivity = gameActivity;
        this.gameTime = 1;

        gameStatus = GameStatus.NOT_STARTED;
        firstRound = true;
        cellsInGame = rows * columns;
        board = new GridLayout(gameActivity);

        createCells();
        drawBoard();
    }

    //RESUME GAME
    public Board(int mineCount, int[][] values, boolean[][] revealed, boolean[][] flagged, GameDifficulty gameDifficulty, GameStatus status, GameActivity gameActivity, long gameTime) {
        this.mineCount = mineCount;
        this.gameDifficulty = gameDifficulty;
        this.gameActivity = gameActivity;
        this.gameTime = gameTime;
        rows = values.length;
        columns = values[0].length;

        cellsInGame = rows * columns;
        gameStatus = status;
        revealedCells = 0;
        flaggedMines = 0;
        flaggedCells = 0;
        firstRound = true;

        cell = new Cell[rows][columns];
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < columns; c++) {
                cell[r][c] = new Cell(r, c, values[r][c], revealed[r][c], flagged[r][c], gameActivity);
                if(cell[r][c].isRevealed()) {
                    revealedCells++;
                    if(cell[r][c].isFlagged()) {
                        flaggedCells++;
                    }
                    firstRound = false;
                } else if(cell[r][c].isFlagged()) {
                    flaggedCells++;
                    if(cell[r][c].isMine()) {
                        flaggedMines++;
                    }
                }
            }
        }

        if(!firstRound) {
            startGameTime();
            findNeighborCells();
            score3BV = new ThreeBV(cell, rows, columns);
            score3BV.calculate3BV();
            // Gack code to fix flag issue
            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < columns; c++) {
                    if(cell[r][c].isFlagged()) {
                        updateNeighborsOfFlagCell(cell[r][c]);
                    }
                }
            }

        }

        gameActivity.boardInfoView.setMineKeeperText(mineCount - flaggedCells);
        gameActivity.boardInfoView.setScoreKeeperText(getGameScore());
        board = new GridLayout(gameActivity);
        drawBoard();
    }

    //MAIN RESUME FOR STATS
    public Board(int mineCount, int[][] values, boolean[][] revealed, boolean[][] flagged, GameStatus status, GameDifficulty gameDifficulty, long gameTime) {
        this.gameTime = gameTime;
        this.mineCount = mineCount;
        this.gameDifficulty = gameDifficulty;
        rows = values.length;
        columns = values[0].length;

        cellsInGame = rows * columns;
        gameStatus = status;
        revealedCells = 0;
        flaggedMines = 0;
        flaggedCells = 0;
        firstRound = true;

        cell = new Cell[rows][columns];
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < columns; c++) {
                cell[r][c] = new Cell(r, c, values[r][c], revealed[r][c], flagged[r][c]);
                if(cell[r][c].isRevealed()) {
                    revealedCells++;
                    if(cell[r][c].isFlagged()) {
                        flaggedCells++;
                    }
                    firstRound = false;
                } else if(cell[r][c].isFlagged()) {
                    flaggedCells++;
                    if(cell[r][c].isMine()) {
                        flaggedMines++;
                    }
                }
            }
        }

        score3BV = new ThreeBV(cell, rows, columns);
        score3BV.calculate3BV();
    }

    // Generates the cells for the Board, they are all empty cells
    private void createCells() {
        cell = new Cell[rows][columns];
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < columns; c++) {
                cell[r][c] = new Cell(r, c, gameActivity);
            }
        }
    }

    public void updateCellSize() {
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < columns; c++) {
                cell[r][c].updateImageValue();
            }
        }
    }
    /*
     * UI & LISTENERS
     */
    // Creates the board for the grid layout
    private void drawBoard() {
        gameActivity.refreshButton.setIcon(R.drawable.ic_action_refresh_playing);
        board.setColumnCount(columns);
        board.setRowCount(rows);

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < columns; c++) {
                board.addView(cell[r][c].getButton());
                if(gameStatus == GameStatus.PLAYING || gameStatus == GameStatus.NOT_STARTED) {
                    setCellTapListeners(cell[r][c]);
                }
            }
        }
    }

    private void setCellTapListeners(Cell tgtCell) {
        final Cell currentCell = tgtCell;

        // Single tap
        currentCell.getButton().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateBoardByTap(currentCell, true);
            }
        });
        // Long tap
        currentCell.getButton().setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                updateBoardByTap(currentCell, false);
                gameActivity.vibrate();
                return true;
            }
        });
        // Touch
//        currentCell.getButton().setOnTouchListener(new View.OnTouchListener() {
//            @Override
//            public boolean onTouch(View arg0, MotionEvent arg1) {
//                if (arg1.getAction() == MotionEvent.ACTION_DOWN) {
//                    gameActivity.refreshButton.setIcon(R.drawable.ic_action_refresh_tap);
//                } else {
//                    gameActivity.refreshButton.setIcon(R.drawable.ic_action_refresh_playing);
//                }
//
//                return false;
//            }
//        });
    }

    //removes the button listeners to make it more efficient
    private void removeCellListeners() {
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < columns; c++) {
                cell[r][c].getButton().setOnClickListener(null);
                cell[r][c].getButton().setOnTouchListener(null);
                cell[r][c].getButton().setOnLongClickListener(null);
            }
        }
    }

    /*
     * CELL CLICKED, USER INPUT
     */
    //updates the game based on the player tapping on a cell
    private void updateBoardByTap(Cell clickedCell, boolean shortTap) {
        boolean longTap = !shortTap;

        if (isQuickChangeTap(shortTap, clickedCell)) {
            gameActivity.changeFlagMode(this);
        } else if(isRevealTap(shortTap, clickedCell)) {
            gameActivity.playSoundEffects(GameSoundType.TAP);
            revealCell(clickedCell);
        } else if (isFlagTap(shortTap)) {
            flagCell(clickedCell);
            if(longTap && !clickedCell.isRevealed()) {
                gameActivity.playSoundEffects(GameSoundType.LONGPRESS);
                clickedCell.getButton().startAnimation(AnimationUtils.loadAnimation(gameActivity, R.anim.puff_in));
            } else {
                gameActivity.playSoundEffects(GameSoundType.TAP);
            }
        }
        checkIfVictorious();

        if(shortTap && gameStatus == GameStatus.PLAYING) {
//            gameActivity.playSoundEffects(GameSoundType.TAP);
        } else if (gameStatus == GameStatus.PLAYING) {
        }
    }

    private boolean isRevealTap(boolean shortTap, Cell clickedCell) {
        boolean longTap = !shortTap;
        return (shortTap && (!gameActivity.getFlagMode() || clickedCell.isRevealed())) || (longTap && gameActivity.getFlagMode());
    }

    private boolean isFlagTap(boolean shortTap) {
        boolean longTap = !shortTap;
        return (shortTap && gameActivity.getFlagMode()) || (longTap && !gameActivity.getFlagMode());
    }

    private boolean isQuickChangeTap(boolean shortTap, Cell clickedCell) {
        return shortTap && UserPrefStorage.getSwiftChange(gameActivity) && clickedCell.getValue() == 0 && clickedCell.isRevealed();
    }

    /*
     * FIRST ROUND UPDATE
     */
    private void setupAfterFirstRound(Cell tgtCell) {
        firstRound = false;
        gameStatus = GameStatus.PLAYING;
        gameActivity.gamePlaying = true;
        startGameTime();

        boolean validBoard = false;

        while (!validBoard) {
            wipeBoard();
            createMines(tgtCell);
            findNeighborCells();
            setAllNeighborValues();
            score3BV = new ThreeBV(cell, rows, columns);
            score3BV.calculate3BV();

            validBoard = score3BV.getThreeBV() > 1;
        }
    }

    private void wipeBoard() {
        for (int r = 0; r < cell.length; r++) {
            for (int c = 0; c < cell[0].length; c++) {
                cell[r][c].setValue(0);
            }
        }
    }

    //creates which cells ( not the firstRound cell) are bombs
    private void createMines(Cell tgtCell) {
        int placedMines = 0;

        while (placedMines != mineCount) {
            int randomR = (int) (Math.random() * rows);
            int randomC = (int) (Math.random() * columns);

            boolean validSpot = true;

            for (int r = tgtCell.getRow() - 1; r <= tgtCell.getRow() + 1; r++) {
                for (int c = tgtCell.getColumn() - 1; c <= tgtCell.getColumn() + 1; c++) {
                    if (inbounds(r, c) && (r == randomR && c == randomC)) {
                        validSpot = false;
                    }
                }
            }

            if (validSpot && !cell[randomR][randomC].isMine()) {
                placedMines++;
                cell[randomR][randomC].setValue(Cell.MINE);
            }
        }
    }

    //finds all the cells that are neighbors to a cell
    private void findNeighborCells() {
        cellNeighbors = new CellNeighbors[cell.length][cell[0].length];
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < columns; c++) {
                cellNeighbors[r][c] = new CellNeighbors(cell, cell[r][c]);
            }
        }
    }

    //calls setNeighborValue for all cells
    private void setAllNeighborValues() {
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < columns; c++) {
                //setNeighborValue(cell[r][c]);
                if(!cell[r][c].isMine()) {
                    cell[r][c].setValue(cellNeighbors[r][c].getNumMines());
                }
            }
        }
    }

    /*
     * FLAG MODE
     */
    //flag or unflag a cell, and update the image
    private void flagCell(Cell tgtCell) {
        if (!tgtCell.isRevealed()) {
            tgtCell.setFlagged(!tgtCell.isFlagged());

            if(tgtCell.isMine()) {
                flaggedMines = (tgtCell.isFlagged()) ? flaggedMines + 1 : flaggedMines - 1;
            }
            flaggedCells = (tgtCell.isFlagged()) ? flaggedCells + 1 : flaggedCells - 1;
            updateNeighborsOfFlagCell(tgtCell);
            tgtCell.updateImageValue();
            gameActivity.boardInfoView.setMineKeeperText((mineCount - flaggedCells));
        }
    }

    //updates the flaggedBomb & flaggedCells vars and finds the icon for the cell to be used
    private void updateNeighborsOfFlagCell(Cell tgtCell) {
        int value = (tgtCell.isFlagged()) ? 1 : -1;

        for (int r = tgtCell.getRow() - 1; r <= tgtCell.getRow() + 1; r++) {
            for (int c = tgtCell.getColumn() - 1; c <= tgtCell.getColumn() + 1; c++) {
                if (inbounds(r, c) && !firstRound) {
                    cellNeighbors[r][c].setNumFlags(cellNeighbors[r][c].getNumFlags() + value);
                }
            }
        }
    }
    /*
     * REVEAL MODE
     */
    //reveals cells based on its value
    //if firstround, it generates the board with bombs and find the difficulty and sets the cells values
    //if not bomb, then reveal
    //if revealed & matched value, then reveal neighbors
    //if cell is bomb, then DEFEAT
    private void revealCell(Cell tgtCell) {
        if (firstRound && !tgtCell.isFlagged()) {
            setupAfterFirstRound(tgtCell);
            updateRevealedCell(tgtCell);
        } else if (revealNonBombCell(tgtCell)) {
            updateRevealedCell(tgtCell);
        } else if (revealRevealedNeighbors(tgtCell)) {
            tappedOnRevealedCell = true;
            revealNeighborCells(tgtCell);
            tappedOnRevealedCell = false;
        } else if (defeatConditions(tgtCell)) {
            gameOver(GameStatus.DEFEAT, tgtCell);
        }
    }

    //updates the image and vars for the cell
    private void updateRevealedCell(Cell tgtCell) {
        revealedCells++;
        tgtCell.setRevealed(true);
        tgtCell.updateImageValue();

        if (tgtCell.getValue() == 0) {
            revealNeighborCells(tgtCell);
        }
    }

    //reveals neighbor cells around a cell
    //This is QUICK OPEN
    private void revealNeighborCells(Cell tgtCell) {
        for (int r = tgtCell.getRow() - 1; r <= tgtCell.getRow() + 1; r++) {
            for (int c = tgtCell.getColumn() - 1; c <= tgtCell.getColumn() + 1; c++) {
                if (inbounds(r, c) && !(tgtCell.getRow() == r && tgtCell.getColumn() == c)) {
                    revealCell(cell[r][c]);
                }
            }
        }
    }

    //Condition to reveal non bomb and non revealed and non flagged cells
    private boolean revealNonBombCell(Cell tgtCell) {
        return !tgtCell.isMine() && !tgtCell.isFlagged() && !tgtCell.isRevealed();
    }

    //Condition to reveal neighbors of revealed cell with flagged neighbor cells that match its value
    private boolean revealRevealedNeighbors(Cell tgtCell) {
        return UserPrefStorage.getSwiftOpen(gameActivity) && !tappedOnRevealedCell && (tgtCell.getValue() != 0)
                && !tgtCell.isFlagged() && tgtCell.isRevealed() && flaggedNeighborEqualsValue(tgtCell);
    }

    //checks if the revealed cell's value matches with flagged bombs, in order to reveal other neighbors
    private boolean flaggedNeighborEqualsValue(Cell tgtCell) {
        int value = tgtCell.getValue();
        int flaggedNeighbors = cellNeighbors[tgtCell.getRow()][tgtCell.getColumn()].getNumFlags();

        return (flaggedNeighbors == value);
    }

    /*
     * GAME OVER
     */
    //CONDITIONS TO WIN OR LOSE
    private boolean victoryConditions() { return (flaggedMines == mineCount && cellsInGame == (flaggedMines + revealedCells))
                                                    || (cellsInGame == (mineCount + revealedCells)); }
    private boolean defeatConditions(Cell tgtCell) { return tgtCell.isMine() && !tgtCell.isFlagged(); }

    //if the player wins do the actions
    private void checkIfVictorious() {
        if (victoryConditions()) {
            gameOver(GameStatus.VICTORY, null);
        }
    }
    //game over actions
    private void gameOver(GameStatus gameStatus, Cell clickedCell) {
        this.gameStatus = gameStatus;
        //play sound
        gameActivity.playSoundEffects((gameStatus == GameStatus.VICTORY) ? GameSoundType.WIN : GameSoundType.LOSE);
        //Update stats/leaderboard/achievements
        if(gameDifficulty != GameDifficulty.CUSTOM) {
            updateLocalStatistics(gameActivity);
            updateGoogleGame();
        }
        //update UI
        int refreshIcon = (gameStatus == GameStatus.VICTORY) ?
                R.drawable.ic_action_refresh_win : R.drawable.ic_action_refresh_lose;
        updateMineImage(gameStatus);
        if(gameStatus == GameStatus.DEFEAT) {
            clickedCell.updateClickedMine();
        }

        removeCellListeners();
        stopGameTime();
        gameActivity.refreshButton.setIcon(refreshIcon);
        gameActivity.vibrate();
    }

    public void gameOverByRestart() {
        gameStatus = GameStatus.DEFEAT;
        //Update stats
        if(gameDifficulty != GameDifficulty.CUSTOM) {
            updateLocalStatistics(gameActivity);
            gameActivity.playSoundEffects(GameSoundType.LOSE);
        }
    }

    public double getGameScore() {
        if(score3BV == null) return 0;
        long time = getGameTime();

        double scoreTemp = (score3BV.getThreeBV() / time);
        return (scoreTemp * 1000.0);
    }

    //reveals the board when you lose
    private void updateMineImage(GameStatus gameStatus) {
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < columns; c++) {
                if(cell[r][c].isMine()) {
                    if (gameStatus == GameStatus.VICTORY) {
                        cell[r][c].setFlagged(true);
                    } else {
                        cell[r][c].setRevealed(true);
                    }
                    cell[r][c].updateImageValue();
                }
            }
        }
    }

    public void updateLocalStatistics(Context context) {

        boolean newBestTime = false;
        boolean newBestScore = false;

        // skip the resume/custom modes
        if(gameDifficulty == GameDifficulty.RESUME || gameDifficulty == GameDifficulty.CUSTOM) return;

        //initial data
        int wins = UserPrefStorage.getWinsForDifficulty(context, gameDifficulty);
        int loses = UserPrefStorage.getLosesForDifficulty(context, gameDifficulty);
        int bestTime = UserPrefStorage.getBestTimeForDifficulty(context, gameDifficulty);
        float avgTime = UserPrefStorage.getAvgTimeForDifficulty(context, gameDifficulty);
        float explorPerct = UserPrefStorage.getExplorPercentForDifficulty(context, gameDifficulty);
        int winStreak = UserPrefStorage.getWinStreakForDifficulty(context, gameDifficulty);
        int losesStreak = UserPrefStorage.getLoseStreakForDifficulty(context, gameDifficulty);
        int currentWinStreak = UserPrefStorage.getCurWinStreakForDifficulty(context, gameDifficulty);
        int currentLosesStreak = UserPrefStorage.getCurLoseStreakForDifficulty(context, gameDifficulty);
        int bestScore = UserPrefStorage.getBestScoreForDifficulty(context, gameDifficulty);
        float avgScore = UserPrefStorage.getAvgScoreForDifficulty(context, gameDifficulty);

        // Update wins/losses/total
        if(gameStatus == GameStatus.VICTORY) {
            wins++;
        } else {
            loses++;
        }
        int total_games = wins + loses;

        // Update best time and avg time
        int currentTime = (int) (getGameTime() / 1000);
        if(gameStatus == GameStatus.VICTORY) {
            // Smaller currentTime is better than bestTime
            if(bestTime > currentTime || bestTime == 0) {
                newBestTime = true;
                bestTime = currentTime;
            }
            avgTime += (currentTime - avgTime)/(wins);
        }

        // Update exploration percentage
        float currentExplorPerct = ((float) revealedCells)/(cellsInGame - mineCount) * 100;
        explorPerct += (currentExplorPerct - explorPerct)/(total_games);

        // Update streaks
        if(gameStatus == GameStatus.VICTORY) {
            currentWinStreak++;
            currentLosesStreak = 0;
        } else {
            currentWinStreak = 0;
            currentLosesStreak++;
        }
        if(currentWinStreak > winStreak)
            winStreak = currentWinStreak;
        if(currentLosesStreak > losesStreak)
            losesStreak = currentLosesStreak;

        // Update best score & avg score
        int currentScore = (int) (getGameScore() * 1000);
        if(gameStatus == GameStatus.VICTORY) {
            // Bigger currentScore is better than bestScore
            if(bestScore < currentScore || bestScore == 0) {
                newBestScore = true;
                bestScore = currentScore;
            }
            avgScore += ((float) currentScore - avgScore)/(wins);
        }

        UserPrefStorage.updateStats(context, gameDifficulty, wins, loses, bestTime, avgTime,
                explorPerct, winStreak, losesStreak, currentWinStreak, currentLosesStreak,
                bestScore, avgScore);

        // Display new bests
        if(newBestTime)
            Toast.makeText(context, R.string.game_best_time, Toast.LENGTH_SHORT).show();

        if(newBestScore)
            Toast.makeText(context, R.string.game_best_score, Toast.LENGTH_SHORT).show();
    }

    private void updateGoogleGame() {
        long millis = getGameTime();
        long score = (long) (getGameScore() * 1000);
        GoogleApiClient googleApiClient = gameActivity.getGoogleApiClient();

        long[] achievementSeconds = {20000,60000,150000};
        String[] achievementWin = {BuildConfig.ACHIEVEMENT_EASY, BuildConfig.ACHIEVEMENT_MEDIUM, BuildConfig.ACHIEVEMENT_EXPERT};
        String[] achievementSpeed = {BuildConfig.ACHIEVEMENT_FAST, BuildConfig.ACHIEVEMENT_QUICK, BuildConfig.ACHIEVEMENT_SWIFT};
        String[] leaderboardScores = {BuildConfig.LEADERBOARD_EASY_BEST_SCORES, BuildConfig.LEADERBOARD_MEDIUM_BEST_SCORES, BuildConfig.LEADERBOARD_EXPERT_BEST_SCORES};
        String[] leaderboardTimes = {BuildConfig.LEADERBOARD_EASY_BEST_TIMES, BuildConfig.LEADERBOARD_MEDIUM_BEST_TIMES, BuildConfig.LEADERBOARD_EXPERT_BEST_TIMES};
        String[] leaderboardStreaks = {BuildConfig.LEADERBOARD_EASY_BEST_STREAK, BuildConfig.LEADERBOARD_MEDIUM_BEST_STREAKs, BuildConfig.LEADERBOARD_EXPERT_BEST_STREAKs};

        if(gameStatus == GameStatus.VICTORY) {
            if(googleApiClient.isConnected()) {
                // Skip non ranked difficulty
                if(gameDifficulty == GameDifficulty.CUSTOM || gameDifficulty == GameDifficulty.RESUME) return;

                // Offset is 2 for RESUME and CUSTOM
                int gameDiffIndex = gameDifficulty.ordinal() - 2;
                Games.Achievements.unlock(googleApiClient, "" + achievementWin[gameDiffIndex]);
                if(millis < achievementSeconds[gameDiffIndex]) {
                    Games.Achievements.unlock(googleApiClient, "" + achievementSpeed[gameDiffIndex]);
                }

                Games.Leaderboards.submitScore(googleApiClient,
                        "" + leaderboardScores[gameDiffIndex],
                        score);
                int seconds = (int) Math.ceil(millis / 1000.0);
                Games.Leaderboards.submitScore(googleApiClient,
                        "" + leaderboardTimes[gameDiffIndex],
                        seconds);
                Games.Leaderboards.submitScore(googleApiClient,
                        "" + leaderboardStreaks[gameDiffIndex],
                        UserPrefStorage.getCurWinStreakForDifficulty(gameActivity, gameDifficulty));
            }
        }
    }

    /*
     * GAME TIME FUNCTIONS
     */

    public void startGameTime() {
        isGameTimerOn = true;
        if(timer != null)
            timer.cancel();
        timer = new Timer();
        TimerTask timerTask = new TimerTask() {
            public void run() {
                gameActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        gameTime += 1000; //increase every sec
                        gameTime -= gameTime % 1000;
                        if (isGameTimerOn) {
                            gameActivity.boardInfoView.setTimeKeeperText(gameTime);
                            gameActivity.boardInfoView.setScoreKeeperText(getGameScore());
                        }
                    }
                });
            }
        };

        timer.scheduleAtFixedRate(timerTask, 0, 1000);
    }

    public void stopGameTime() {
        isGameTimerOn = false;
        if(timer != null)
            timer.cancel();
    }

    /*
     * HELPER METHODS
     */
    // Checks if the cell being called is inbounds
    private boolean inbounds(int row, int column) {
        return (0 <= row && row < rows && 0 <= column && column < columns);
    }

    public GridLayout getLayout() { return board; }
    public GameStatus getGameStatus() { return gameStatus; }
    public GameDifficulty getGameDifficulty() { return gameDifficulty; }
    public int getRows() { return rows; }
    public int getColumns() { return columns; }
    public int getMineCount() { return mineCount; }
    public int getCellValue(int r, int c) { return cell[r][c].getValue(); }
    public boolean getCellReveal(int r, int c) { return cell[r][c].isRevealed(); }
    public boolean getCellFlag(int r, int c) { return cell[r][c].isFlagged(); }
    public boolean getFirstRound() { return firstRound; }
    public long getGameTime() {
        return (gameTime == 0) ? 1 : gameTime;
    }
}

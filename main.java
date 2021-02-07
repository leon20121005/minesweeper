package minesweeper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Main main = new Main();
        System.out.println("How many mines do you want on the field?");
        Scanner scanner = new Scanner(System.in);
        int numberOfMines = Integer.valueOf(scanner.nextLine());
        MinesweeperGame game = main.new MinesweeperGame(numberOfMines);
        game.execute();
    }

    private class MinesweeperGame {
        private Field field;
        private boolean isGameOver = false;

        public MinesweeperGame(int numberOfMines) {
            this.field = new Field(numberOfMines);
        }

        public void execute() {
            this.field.print();
            while (!this.isGameOver) {
                ICommand command = this.getCommand(this.field);
                if (command == null || !command.execute()) {
                    continue;
                }
                this.field.print();
                if (!this.field.areAllMinesCovered()) {
                    System.out.println("You stepped on a mine and failed!");
                    this.isGameOver = true;
                    continue;
                }
                if (this.field.areAllMinesMarked() || this.field.areAllDirtsCovered()) {
                    System.out.println("Congratulations! You found all the mines!");
                    this.isGameOver = true;
                    continue;
                }
            }
        }

        private ICommand getCommand(Field field) {
            System.out.println("Set/unset mines marks or claim a cell as free:");
            Scanner scanner = new Scanner(System.in);
            String[] input = scanner.nextLine().split(" ");
            int x = Integer.valueOf(input[1]) - 1;
            int y = Integer.valueOf(input[0]) - 1;
            return new CommandFactory().createCommand(input[2], field, x, y);
        }
    }

    private interface ICommand {
        public boolean execute();
    }

    private class CommandSwitchMark implements ICommand {
        private Field field;
        private int x;
        private int y;

        public CommandSwitchMark(Field field, int x, int y) {
            this.field = field;
            this.x = x;
            this.y = y;
        }

        @Override
        public boolean execute() {
            if (!this.field.isTileAvailable(this.x, this.y)) {
                System.out.println("It is outside the field or already uncovered!");
                return false;
            }
            if (!this.field.switchMark(this.x, this.y)) {
                System.out.println("The number of marked cells can't be more than mines!");
                return false;
            }
            return true;
        }
    }

    private class CommandUncoverTile implements ICommand {
        private Field field;
        private int x;
        private int y;

        public CommandUncoverTile(Field field, int x, int y) {
            this.field = field;
            this.x = x;
            this.y = y;
        }

        @Override
        public boolean execute() {
            if (!this.field.isTileAvailable(this.x, this.y)) {
                System.out.println("It is outside the field or already uncovered!");
                return false;
            }
            return this.field.uncoverTile(this.x, this.y);
        }
    }

    private class CommandFactory {
        public ICommand createCommand(String commandType, Field field, int x, int y) {
            switch (commandType) {
                case "mine":
                    return new CommandSwitchMark(field, x, y);
                case "free":
                    return new CommandUncoverTile(field, x, y);
                default:
                    return null;
            }
        }
    }

    private class Field {
        private int numberOfMines;
        private int numberOfMarks;
        private Tile[][] tiles;
        private Mine[] mines;
        private Dirt[] dirts;

        public Field(int numberOfMines) {
            this.numberOfMines = numberOfMines;
            this.tiles = new Tile[9][9];
            this.mines = new Mine[this.numberOfMines];
            this.dirts = new Dirt[81 - this.numberOfMines];
            // fill the minefield
            List<Integer> randomIndexes = this.generateRandomIndexes();
            for (int index = 0; index < 81; index++) {
                int x = randomIndexes.get(index) / 9;
                int y = randomIndexes.get(index) % 9;
                if (index < this.numberOfMines) {
                    this.mines[index] = new Mine();
                    this.tiles[x][y] = this.mines[index];
                }
                else {
                    this.dirts[index - this.numberOfMines] = new Dirt();
                    this.tiles[x][y] = this.dirts[index - this.numberOfMines];
                }
            }
            this.countAdjacentMines(randomIndexes);
        }

        private List<Integer> generateRandomIndexes() {
            List<Integer> randomIndexes = new ArrayList<>();
            for (int index = 0; index < 81; index++) {
                randomIndexes.add(index);
            }
            Collections.shuffle(randomIndexes);
            return randomIndexes;
        }

        private void countAdjacentMines(List<Integer> randomIndexes) {
            for (int index = 0; index < this.numberOfMines; index++) {
                int x = randomIndexes.get(index) / 9;
                int y = randomIndexes.get(index) % 9;
                int[] delta = {1, -1, 0};
                for (int xDelta: delta) {
                    for (int yDelta: delta) {
                        if (!(xDelta == 0 && yDelta == 0) && this.isTileInsideField(x + xDelta, y + yDelta)) {
                            this.tiles[x + xDelta][y + yDelta].increaseNumberOfAdjacentMines();
                        }
                    }
                }
            }
        }

        public void print() {
            System.out.println(" |123456789|");
            System.out.println("—│—————————│");
            for (int index = 0; index < this.tiles.length; index++) {
                System.out.print(index + 1 + "│");
                for (Tile tile: this.tiles[index]) {
                    System.out.print(tile.getRepresentation());
                }
                System.out.println("│");
            }
            System.out.println("—│—————————│");
        }

        public boolean isTileAvailable(int x, int y) {
            return this.isTileInsideField(x, y) && this.tiles[x][y].isCovered();
        }

        public boolean switchMark(int x, int y) {
            if (!this.tiles[x][y].isMarked() && this.numberOfMarks == this.numberOfMines) {
                return false;
            }
            if (!this.tiles[x][y].isMarked()) {
                this.tiles[x][y].setMarked(true);
                this.numberOfMarks += 1;
            }
            else {
                this.tiles[x][y].setMarked(false);
                this.numberOfMarks -= 1;
            }
            return true;
        }

        public boolean uncoverTile(int x, int y) {
            if (this.isTileDirt(x, y)) {
                this.uncoverAdjacentDirts(x, y);
            }
            else {
                this.setAllMinesUncovered();
            }
            return true;
        }

        private boolean isTileInsideField(int x, int y) {
            return x > -1 && x < 9 && y > -1 && y < 9;
        }

        private boolean isTileDirt(int x, int y) {
            return this.tiles[x][y] instanceof Dirt;
        }

        private void uncoverAdjacentDirts(int x, int y) {
            if (!(this.isTileAvailable(x, y) && this.isTileDirt(x, y))) {
                return;
            }
            this.setTileUncovered(this.tiles[x][y]);
            if (this.tiles[x][y].isAdjacentToMine()) {
                return;
            }
            this.uncoverAdjacentDirts(x, y - 1);
            this.uncoverAdjacentDirts(x, y + 1);
            this.uncoverAdjacentDirts(x - 1, y);
            this.uncoverAdjacentDirts(x + 1, y);
        }

        private void setTileUncovered(Tile tile) {
            tile.setMarked(false);
            tile.setCovered(false);
            this.numberOfMarks -= 1;
        }

        private void setAllMinesUncovered() {
            for (Mine mine: this.mines) {
                this.setTileUncovered(mine);
            }
        }

        public boolean areAllMinesCovered() {
            for (Mine mine: this.mines) {
                if (!mine.isCovered()) {
                    return false;
                }
            }
            return true;
        }

        public boolean areAllMinesMarked() {
            for (Mine mine: this.mines) {
                if (!mine.isMarked()) {
                    return false;
                }
            }
            return true;
        }

        public boolean areAllDirtsCovered() {
            for (Dirt dirt: this.dirts) {
                if (!dirt.isCovered()) {
                    return false;
                }
            }
            return true;
        }
    }

    private enum Representation {
        COVERED('.'), MARKED('*');
        private final char representation;
        Representation(char representation) {
            this.representation = representation;
        }
        public char getValue() {
            return this.representation;
        }
    }

    private abstract class Tile {
        private boolean isMarked = false;
        private boolean isCovered = true;
        public int numberOfAdjacentMines = 0;
        protected char representation;

        public Tile() {
        }

        public boolean isMarked() {
            return this.isMarked;
        }

        public void setMarked(boolean isMarked) {
            this.isMarked = isMarked;
        }

        public boolean isCovered() {
            return this.isCovered;
        }

        public void setCovered(boolean isCovered) {
            this.isCovered = isCovered;
        }

        public void increaseNumberOfAdjacentMines() {
            this.numberOfAdjacentMines += 1;
        }

        public boolean isAdjacentToMine() {
            if (this.numberOfAdjacentMines != 0) {
                return true;
            }
            return false;
        }

        public char getRepresentation() {
            if (this.isMarked) {
                return Representation.MARKED.getValue();

            }
            if (this.isCovered) {
                return Representation.COVERED.getValue();
            }
            // return the default value
            return Character.MIN_VALUE;
        }
    }

    private class Mine extends Tile {
        public Mine() {
            this.representation = 'X';
        }

        @Override
        public char getRepresentation() {
            if (super.getRepresentation() != Character.MIN_VALUE) {
                return super.getRepresentation();
            }
            return this.representation;
        }
    }

    private class Dirt extends Tile {
        public Dirt() {
            this.representation = '/';
        }

        @Override
        public char getRepresentation() {
            if (super.getRepresentation() != Character.MIN_VALUE) {
                return super.getRepresentation();
            }
            if (this.numberOfAdjacentMines == 0) {
                return this.representation;
            }
            return (char) (this.numberOfAdjacentMines + '0');
        }
    }
}

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.NoSuchElementException;
import java.util.Random;
import tester.*;
import javalib.impworld.*;
import javalib.worldimages.AlignModeX;
import javalib.worldimages.AlignModeY;
import javalib.worldimages.EmptyImage;
import javalib.worldimages.OutlineMode;
import javalib.worldimages.OverlayOffsetAlign;
import javalib.worldimages.Posn;
import javalib.worldimages.RectangleImage;
import javalib.worldimages.RotateImage;
import javalib.worldimages.WorldImage;

//represents an ICell, which makes up a maze
interface ICell {
  // to attempt draw a cell and its surrounding cells
  // EFFECT: Mutates the drawGraph object, which represents the visual maze.
  void draw(DrawGraph drawGraph, boolean firstCall, int mode, int furthestFromOrigin);

  // to attempt to add a cell to a current worklist if it has not been visited
  // EFFECT: Mutates the worklist to include this cell if has not been visited
  void addToList(ICollection<Cell> worklist);

  // to return the PosnID for a given cell, this is NECESSARY to connect the
  // HashMap with the cells
  Posn getId();

  // to edit the distance from the the field edge, used when creating HeatMap
  // mutates the current distance, and is recursed on all neighbors with increased
  // distance
  int updateNeighborDistance(Edge from, int distance);

  // to attempt to change color state of a cell, or to highlight it, and recurses
  // onto previous values in order to show the correct path at the end of search
  // algorithms
  // or player maze finishes
  // EFFECT: mutates the current cell to be highlighted, and all previous cells
  void highlight();

  // with a given ICell, attempts to set the previous of current cell to that
  // EFFECT: attempts to mutate the previous cell depending on class definition to
  // the
  // current cell
  void updateCellHistory(ICell previous);

  // to return and possibly highlight the last Cell from specfied ICell
  Cell movedFrom(Cell from);
}

// Represents a single square in a maze, with all connected edges and qualities
class Cell implements ICell {

  private Edge up;
  private Edge down;
  private Edge left;
  private Edge right;
  private final Posn id;
  private final Color color;
  private ICell previouslyAccessed;
  private int distance; // distance from origin, either topLeft or bottomRight
  private boolean visited; // has this cell been visited by a search
  private boolean highlighted; // is the cell part of the highlighted path

  Cell(Posn id) {
    this.id = id;
    this.color = Color.GRAY;
    this.distance = 0;
    this.visited = false;
    this.highlighted = false;
    this.previouslyAccessed = new MtCell();
  }

  // to update left Edge with given Edge
  // EFFECT: mutates left field to a specified edge
  void updateLeftEdge(Edge left) {
    this.left = left;
  }

  // to update right Edge with given Edge
  // EFFECT: mutates right field to a specified edge
  void updateRightEdge(Edge right) {
    this.right = right;
  }

  // to update up Edge with given Edge
  // EFFECT: mutates up field to a specified edge
  void updateUpEdge(Edge up) {
    this.up = up;
  }

  // to update down Edge with given Edge
  // EFFECT: mutates down field to a specified edge
  void updateDownEdge(Edge down) {
    this.down = down;
  }

  // meant to be used in the manual mode of the
  // maze. The given Cell is the previously current
  // cell of the player. now, this cell is being moved to
  // so highlight this cell and return it as the new current cell
  public Cell movedFrom(Cell from) {
    this.highlighted = true;
    return this;
  }

  // to return the PosnID for a given cell, this is NECESSARY to connect the
  // HashMap with the cells
  public Posn getId() {
    return this.id;
  }

  // to attempt draw a cell and its surrounding cells
  // EFFECT: mutates the drawGraph object, which represents the visual maze.
  public void draw(DrawGraph cameFrom, boolean firstCall, int mode,
      int furthestFromOrigin) {

    Color color;

    // highlighted takes priority
    if (this.highlighted) {
      color = new Color(3, 252, 198);
    }
    // then visited
    else if (this.visited) {
      color = new Color(50, 168, 129);
    }
    // then heatmap
    else if (mode == 1) {
      double scale = (double) (this.distance) / (double) (furthestFromOrigin);
      color = new Color((int) (255 * scale), 0, (int) (255 * (1 - scale)));
    }
    // then just this color
    else {
      color = this.color;
    }

    cameFrom.drawCellRow(this, this.id, this.left, this.right, this.up, this.down,
        color, firstCall,
        mode);
  }

  // to add all non visted neighbors of a single cell to a worklist,
  // and will update respective nodes with their previouslyAccessed cells
  void addNeighborsToWorklist(ICollection<Cell> worklist) {
    this.visited = true;
    // adds the neighbors around to worklist
    this.left.addNeighborToWorklist(worklist, this);
    this.right.addNeighborToWorklist(worklist, this);
    this.up.addNeighborToWorklist(worklist, this);
    this.down.addNeighborToWorklist(worklist, this);

    // updates the cell history of neighbors to be this cell, used
    // to retrace the optimal path
    this.up.updateNeighborCellHistory(this);
    this.down.updateNeighborCellHistory(this);
    this.left.updateNeighborCellHistory(this);
    this.right.updateNeighborCellHistory(this);
  }

  // to move upwards through the upwards edge to the neighboring
  // ICell. If the move isn't successful, this cell is returned
  // otherwise, the moved to cell is return
  Cell moveUp() {
    return this.move(this.up);
  }

  // to move downwards through the upwards edge to the neighboring
  // ICell, returning Cell if valid.
  Cell moveDown() {
    return this.move(this.down);
  }

  // to move leftwards through the upwards edge to the neighboring
  // ICell, returning Cell if valid.
  Cell moveLeft() {
    return this.move(this.left);
  }

  // to move rightwards through the upwards edge to the neighboring
  // ICell, returning Cell if valid.
  Cell moveRight() {
    return this.move(this.right);
  }

  // to move towards a specific edge, returning the connected edge if valid
  // EFFECT: updates the edge's neighbor history, and sets current cell moved
  // from to visited, as it has been accessed.
  private Cell move(Edge e) {
    this.visited = true;
    Cell toReturn = e.move(this);
    // if the move was unsuccessful,
    // just return this
    if (toReturn == this) {
      return this;
    } else {
      // otherwise, update the cell history of the given edge to include this,
      // and remove the highlight from this
      e.updateNeighborCellHistory(this);
      this.highlighted = false;
      return toReturn;
    }
  }

  // to add cell to a given ICollection
  // EFFECT: depending on worklist implementation, adds current Cell
  // ICollection if cell has not been visited
  public void addToList(ICollection<Cell> worklist) {
    if (!this.visited) {
      worklist.add(this);
    }
  }

  // to change the previous cell to a given ICell
  // EFFECT: mutates previouslyAccesses to a given previous cell
  // if current has not been visited (used to preserve the correct path)
  public void updateCellHistory(ICell prev) {
    if (!this.visited) {
      this.previouslyAccessed = prev;
    }
  }

  // to highlight current cell and all previous cells
  // EFFECT: mutates current and all previous cell's highlighted state to true
  public void highlight() {
    this.highlighted = true;
    this.previouslyAccessed.highlight();
  }

  // to calculate the distance of all neighbors to the current cell
  public int calculateDistanceFromThisCell() {
    return this.updateNeighborDistance(null, 0);
  }

  // to calculate the distance from the current cell to the
  // calculateDistanceFromCell()
  // specified cell.
  // topLeft or bottomRight
  // EFFECT: mutates current distance to specified distance in field, which is
  // recursive
  // on all valid neighbors, increasing in distance according to how far away from
  // specified cell
  // returns the furthest distance from the origin
  public int updateNeighborDistance(Edge cameFrom, int distance) {

    this.distance = distance;
    int maxDistance = this.distance;
    if (this.up != cameFrom) {
      maxDistance = Math.max(this.up.updateNeighborDistance(this, distance + 1),
          maxDistance);
    }
    if (this.down != cameFrom) {
      maxDistance = Math.max(this.down.updateNeighborDistance(this, distance + 1),
          maxDistance);
    }
    if (this.left != cameFrom) {
      maxDistance = Math.max(this.left.updateNeighborDistance(this, distance + 1),
          maxDistance);
    }
    if (this.right != cameFrom) {
      maxDistance = Math.max(this.right.updateNeighborDistance(this, distance + 1),
          maxDistance);
    }

    return maxDistance;
  }
}

// Represents the edge of a maze, with a cell that cannot be moved onto
class MtCell implements ICell {
  // to draw a empty cell
  // EFFECT: doesnt mutate anything, as it indicates the end of a maze
  public void draw(DrawGraph drawGraph, boolean firstCall, int mode,
      int furthestFromOrigin) {
    return;
  }

  // to throw an exception when called, as there is no valid access onto the edge
  // of a maze
  public Posn getId() {
    throw new IllegalAccessError();
  }

  // to not do anything, as the edge of the maze should not be processed
  // in the worklist
  // EFFECT: doesnt change anything, as empty cells should not be added to list
  public void addToList(ICollection<Cell> worklist) {
    return;
  }

  // to return the distance away from given came from cell,
  // ends the maze distance calculation
  public int updateNeighborDistance(Edge cameFrom, int distance) {
    return distance;
  }

  // to not highlight a empty cell, as you are unable to move onto empty cells,
  // so therefore they will never be included into highlighted solution
  // EFFECT: does nothing, as you are unable to highlight a empty cell
  public void highlight() {
    return;
  }

  // to not update the cell history, as this indicates the end of a maze
  // representation
  // tracking the previous ICell is unnecessary
  // EFFECT: does nothing, as a empty cell should not need to keep track of
  // previous cells
  public void updateCellHistory(ICell previous) {
    return;
  }

  // to return the latest Cell that was moved from,
  // which in this case is the given field cell
  public Cell movedFrom(Cell from) {
    return from;
  }

}

// Represents the Edge in a graph representation
class Edge {

  private final ICell cell1;
  private final ICell cell2;
  // weight for sorting
  private final int weight;
  // walled if the two cells can travel to eachother
  private boolean walled;

  Edge(ICell cell1, ICell cell2, int weight) {
    this.cell1 = cell1;
    this.cell2 = cell2;
    this.walled = true;
    this.weight = weight;
  }

  // to return the opposite cell of a edge on a given Cell
  private ICell getOpposite(ICell other) {
    if (other == cell1) {
      return cell2;
    } else {
      return cell1;
    }
  }

  // to draw a representation of a wall if the given edge is walled or not,
  // uses the cellDimensions to scale
  WorldImage drawWall(int cellDimensions) {
    if (walled) {
      int wallWidth = (int) (cellDimensions * .1);
      return new RectangleImage(wallWidth, cellDimensions, OutlineMode.SOLID,
          Color.BLACK);
    }

    return new EmptyImage();
  }

  // to mutate the drawGraph through generating all right cells from a specified
  // edge and cameFrom ICell
  void drawRightCells(ICell cameFrom, DrawGraph drawGraph, boolean firstCall, int mode,
      int furthestFromOrigin) {

    this.getOpposite(cameFrom).draw(drawGraph, firstCall, mode, furthestFromOrigin);

  }

  // Updates all neighbored cells depending if a given edge is walled or not.
  // if a given edge is walled, stops the recursion and returns the distance from
  // current spot, which is then recursed on neighbors that are not walled.
  // After recursion, returns the distance of the current spot in relation to the
  // accumulated in correspondence in the from cell.
  int updateNeighborDistance(ICell from, int distance) {
    if (this.walled) {
      return distance;
    }

    return this.getOpposite(from).updateNeighborDistance(this, distance);
  }

  // to remove the wall on a edge, which represents a edge in between
  // two cells
  // EFFECT: mutates the wall representation to false, "breaking the wall"
  void breakWall() {
    this.walled = false;
  }

  // to compare the two given edges, returning int value representing
  // how the two measure up
  int compare(Edge other) {
    return this.weight - other.weight;
  }

  // with a given edge, add the ids of from and to into a given hashmap
  // EFFECT: mutates a given HashMap, replacing any existing cell ids with
  // the ones in the provided edge
  void addNodes(HashMap<Posn, Posn> map) {
    map.put(cell1.getId(), cell1.getId());
    map.put(cell2.getId(), cell2.getId());
  }

  // to connect the specified edge in the kruskals algorithm
  // EFFECT: mutates the specified edge by breaking the wall,
  // and to unionize two cells or trees if valid to connect
  void kruskalsHelper(KruskalsUnionFind kuf) {
    kuf.connectCells(this, this.cell1, this.cell2);
  }

  // to add the neighbor of a given ICell from, and checks the current edge see if
  // valid
  // EFFECT: mutates the given ICollection to include a neighbor of a given ICell
  // if this edge is walled, this indicates that you cannot add a neighbor
  void addNeighborToWorklist(ICollection<Cell> worklist, ICell from) {
    if (!this.walled) {
      this.getOpposite(from).addToList(worklist);
    }
  }

  // to update the opposite of the from ICell, and updates the from cell
  // if the edge is walled, doesnt do anything as this indicates that you cannot
  // move through this edge
  // EFFECT: to mutate the cell history of the neighbor of the from Cell
  // if the edge is able to be passed through
  void updateNeighborCellHistory(ICell from) {
    if (!this.walled) {
      this.getOpposite(from).updateCellHistory(from);
    }
  }

  // to return the cell that is being moved onto
  // preventing the move if the edge is walled, which indicates
  // that you cannot pass through the edge.
  Cell move(Cell from) {
    if (!this.walled) {
      return this.getOpposite(from).movedFrom(from);
    } else {
      return from;
    }
  }

}

// To represent a graph of a maze
class Graph {

  private Cell topLeft;
  private Cell bottomRight;
  private Cell currentlyOn;
  private int width;
  private int height;
  private IAnimationHelper animationHelper;
  private boolean manualMode;
  // (EXTRA CREDIT) increase in bias will cause more vertical columns
  private double verticalBias;
  // (EXTRA CREDIT) increase in bias will cause more horizontal
  // columns
  private double horizontalBias;

  Graph(int width, int height, double horizontalBias, double verticalBias) {
    this.verticalBias = verticalBias;
    this.horizontalBias = horizontalBias;
    this.initialize(width, height, (int) (Math.random() * 1000));
  }

  // test constructor, which takes in a seed for predictive randomness
  Graph(int width, int height, int seed, double horizontalBias, double verticalBias) {
    this.verticalBias = horizontalBias;
    this.horizontalBias = verticalBias;
    this.initialize(width, height, seed);
  }

  // to create a random maze generation with a given height, width and random seed
  // EFFECT: mutates all fields in the graph, and generates a random maze,
  // excluding the
  // vertical and horizontal biases, which are specified in the constructor
  void initialize(int width, int height, int seed) {
    if (width > 100 || height > 60) {
      throw new IllegalArgumentException("Maze is too big no goood");
    }

    // This nested for loop generates all of the possible cells given
    // the width and height
    ArrayList<ArrayList<Cell>> cells = new ArrayList<>();

    // for each row in the height, create the cells in that row
    for (int rowIndex = 0; rowIndex < height; rowIndex += 1) {

      cells.add(new ArrayList<>());
      // for each column in that row, create a new cell
      for (int colIndex = 0; colIndex < width; colIndex += 1) {
        cells.get(rowIndex).add(new Cell(new Posn(colIndex, rowIndex)));
      }
    }
    // set the top left (starting cell)
    this.topLeft = cells.get(0).get(0);
    // set the bottom right (ending cell)
    this.bottomRight = cells.get(cells.size() - 1).get(cells.get(0).size() - 1);
    // initializes current cell as topLeft
    this.currentlyOn = this.topLeft;

    // creates the edges
    ArrayList<Edge> edges = new Utils().createEdges(cells, seed, this.verticalBias,
        this.horizontalBias);

    // create spanning tree
    KruskalsUnionFind kuf = new KruskalsUnionFind(edges);
    kuf.putEdges();
    // animation helper to help with states in the world class
    this.animationHelper = kuf;
    this.height = height;
    this.width = width;

    this.manualMode = false;
  }

  // to reinitialize the graph representation with the same height and width
  // EFFECT: mutates all of the fields to a new random maze (excluding biases)
  void reset() {
    this.initialize(this.width, this.height, (int) (Math.random() * 1000));
  }

  // to draw the graph, given a specific color mode (EXTRA CREDIT), will return
  // a complete scene of a graph turned into a maze
  WorldScene draw(int mode) {
    if (!this.animationHelper.isFinished()) {
      this.animationHelper.iterate();
    }

    // optimal cellDimensions based on the width and height bounds of the window
    int cellDimensions = Math.min((int) (1500 / this.width), (int) (750 / this.height));

    WorldScene scene = new WorldScene(cellDimensions * this.width, cellDimensions * this.height);
    DrawGraph drawGraph = new DrawGraph(scene, cellDimensions, this.topLeft, this.bottomRight);
    drawGraph.drawCells(mode);
    return scene;
  }

  // to represent the breadth first search algorithm
  // EFFECT: mutates the given graph visitation fields to represent a
  // breadth first search
  void bfs() {
    if (this.animationHelper.isFinished() && !this.manualMode) {
      this.animationHelper = new SearchHelper(this.topLeft, this.bottomRight, new Queue<Cell>());
    }
  }

  // to represent the depth first search algorithm
  // EFFECT: mutates the given graph visitation fields to represent a
  // depth first search
  void dfs() {
    if (this.animationHelper.isFinished() && !this.manualMode) {
      this.animationHelper = new SearchHelper(this.topLeft, this.bottomRight, new Stack<Cell>());
    }
  }

  // to start the manual mode of a Graph representation,
  // EFFECT: mutates the topLeft cell, indicating the maze starting point visually
  void startManualMode() {
    this.topLeft.highlight();
    this.manualMode = true;
  }

  // to change graph in accordance to movement input in manual mode
  // EFFECT: depending if graph is on manual mode, mutates visitation and
  // highlighting on cells for actions of a player moving around the maze.
  void move(String direction) {
    if (this.manualMode) {
      if (direction.equals("up")) {
        this.currentlyOn = this.currentlyOn.moveUp();
      } else if (direction.equals("down")) {
        this.currentlyOn = this.currentlyOn.moveDown();

      } else if (direction.equals("left")) {
        this.currentlyOn = this.currentlyOn.moveLeft();

      } else if (direction.equals("right")) {
        this.currentlyOn = this.currentlyOn.moveRight();
      }

      if (this.currentlyOn == this.bottomRight) {
        this.currentlyOn.highlight();
        this.manualMode = false;
      }
    }
  }

  // to skip a given animation sequence
  // EFFECT: mutates the animationHelper finished field to skip all
  // tick movements in a search sequence.
  void skipAnimation() {
    while (!this.animationHelper.isFinished()) {
      this.animationHelper.iterate();
    }
  }
}

// Represents the full game of a Maze game
class MazeGame extends World {

  private final Graph graph;
  private int mode;

  MazeGame(Graph g) {
    this.graph = g;
    this.mode = 0;
  }

  // to create the complete WorldScene by drawing the graph field
  public WorldScene makeScene() {
    return this.graph.draw(this.mode);
  }

  // to process key events in the given key events
  // EFFECT: depending on key press, delegates mutation
  // to specified void method, which mutates graph directly
  public void onKeyEvent(String key) {
    if (key.equals("d")) {
      // Depth First Search
      this.graph.dfs();

    } else if (key.equals("b")) {
      // Breadth First Search
      this.graph.bfs();
    } else if (key.equals("m")) {
      // Start Manual Mode
      this.graph.startManualMode();
    }
    // Change Color Mode,
    // 0 for normal maze,
    // 1 for gradient from start,
    // 2 for gradient from ending
    else if (key.equals("c")) {
      if (this.mode == 0 || this.mode == 1) {
        this.mode += 1;
      } else if (this.mode == 2) {
        this.mode = 0;
      }
    } else if (key.equals("r")) {
      this.graph.reset(); // Reset graph, recreating a random maze
    } else if (key.equals("s")) {
      this.graph.skipAnimation(); // Skip current animation
    } else {
      this.graph.move(key); // moves if in manual mode & if valid key
    }
  }
}

class Examples {

  void test100By60Maze(Tester t) {
    Graph g = new Graph(100, 60, 1, 1);
    MazeGame lol = new MazeGame(g);
    lol.bigBang(1500, 1000, .00000000000001);
  }

  // void test100By60MazeVerticalBias(Tester t) {
  // Graph g = new Graph(100, 60, 1, 100);
  // MazeGame lol = new MazeGame(g);
  // lol.bigBang(1500, 1000, .00000000000001);
  // }

  // void test100By60MazeHorizontalBias(Tester t) {
  // Graph g = new Graph(100, 60, 100, 1);
  // MazeGame lol = new MazeGame(g);
  // lol.bigBang(1500, 1000, .00000000000001);
  // }

  // void test10By10MazeForManualMode(Tester t) {
  // Graph g = new Graph(10, 10, 1, 1);
  // MazeGame lol = new MazeGame(g);
  // lol.bigBang(1500, 1000, .00000000000001);
  // }

  // Data for a 3 by 3 maze
  MtCell empty;
  Cell topLeft;
  Cell topMiddle;
  Cell topRight;
  Cell middleLeft;
  Cell middleMiddle;
  Cell middleRight;
  Cell bottomLeft;
  Cell bottomMiddle;
  Cell bottomRight;

  ArrayList<ArrayList<Cell>> cells;

  // only contains edges between two Cells, not MtCells
  ArrayList<Edge> edges;

  Random r;

  Graph graph;
  MazeGame game;

  void init() {
    this.cells = new ArrayList<>();
    cells.add(new ArrayList<>());
    cells.add(new ArrayList<>());
    cells.add(new ArrayList<>());

    this.topLeft = new Cell(new Posn(0, 0));
    cells.get(0).add(topLeft);
    this.topMiddle = new Cell(new Posn(1, 0));
    cells.get(0).add(topMiddle);
    this.topRight = new Cell(new Posn(2, 0));
    cells.get(0).add(topRight);

    this.middleLeft = new Cell(new Posn(0, 1));
    cells.get(1).add(middleLeft);

    this.middleMiddle = new Cell(new Posn(1, 1));
    cells.get(1).add(middleMiddle);

    this.middleRight = new Cell(new Posn(2, 1));
    cells.get(1).add(middleRight);

    this.bottomLeft = new Cell(new Posn(0, 2));
    cells.get(2).add(bottomLeft);

    this.bottomMiddle = new Cell(new Posn(1, 2));
    cells.get(2).add(bottomMiddle);

    this.bottomRight = new Cell(new Posn(2, 2));
    cells.get(2).add(bottomRight);

    this.r = new Random(100);
    this.graph = new Graph(3, 3, 100, 1, 1);
    this.game = new MazeGame(this.graph);
  }

  void initEdges() {
    this.edges = new ArrayList<>();
    Edge e1 = new Edge(topLeft, new MtCell(), 0);
    topLeft.updateUpEdge(e1);

    Edge e2 = new Edge(topLeft, topMiddle, r.nextInt(1000));
    topLeft.updateRightEdge(e2);
    topMiddle.updateLeftEdge(e2);
    edges.add(e2);

    Edge e3 = new Edge(topLeft, new MtCell(), 0);
    topLeft.updateLeftEdge(e3);

    Edge e4 = new Edge(topMiddle, new MtCell(), 0);
    topMiddle.updateUpEdge(e4);

    Edge e5 = new Edge(topMiddle, topRight, r.nextInt(1000));
    topMiddle.updateRightEdge(e5);
    topRight.updateLeftEdge(e5);
    edges.add(e5);

    Edge e6 = new Edge(topRight, new MtCell(), 0);
    topRight.updateUpEdge(e6);

    Edge e7 = new Edge(topRight, new MtCell(), 0);
    topRight.updateRightEdge(e7);

    Edge e8 = new Edge(middleLeft, topLeft, r.nextInt(1000));
    middleLeft.updateUpEdge(e8);
    topLeft.updateDownEdge(e8);
    edges.add(e8);

    Edge e9 = new Edge(middleLeft, middleMiddle, r.nextInt(1000));
    middleLeft.updateRightEdge(e9);
    middleMiddle.updateLeftEdge(e9);
    edges.add(e9);

    Edge e10 = new Edge(middleLeft, new MtCell(), 0);
    middleLeft.updateLeftEdge(e10);

    Edge e11 = new Edge(middleMiddle, topMiddle, r.nextInt(1000));
    middleMiddle.updateUpEdge(e11);
    topMiddle.updateDownEdge(e11);
    edges.add(e11);

    Edge e12 = new Edge(middleMiddle, middleRight, r.nextInt(1000));
    middleMiddle.updateRightEdge(e12);
    middleRight.updateLeftEdge(e12);
    edges.add(e12);

    Edge e13 = new Edge(middleRight, topRight, r.nextInt(1000));
    middleRight.updateUpEdge(e13);
    topRight.updateDownEdge(e13);
    edges.add(e13);

    Edge e14 = new Edge(middleRight, new MtCell(), 0);
    middleRight.updateRightEdge(e14);

    Edge e15 = new Edge(bottomLeft, middleLeft, r.nextInt(1000));
    bottomLeft.updateUpEdge(e15);
    middleLeft.updateDownEdge(e15);
    edges.add(e15);

    Edge e16 = new Edge(bottomLeft, bottomMiddle, r.nextInt(1000));
    bottomLeft.updateRightEdge(e16);
    bottomMiddle.updateLeftEdge(e16);
    edges.add(e16);

    Edge e17 = new Edge(bottomLeft, new MtCell(), 0);
    bottomLeft.updateLeftEdge(e17);

    Edge e18 = new Edge(bottomLeft, new MtCell(), 0);
    bottomLeft.updateDownEdge(e18);

    Edge e19 = new Edge(bottomMiddle, middleMiddle, r.nextInt(1000));
    bottomMiddle.updateUpEdge(e19);
    middleMiddle.updateDownEdge(e19);
    edges.add(e19);

    Edge e20 = new Edge(bottomMiddle, bottomRight, r.nextInt(1000));
    bottomMiddle.updateRightEdge(e20);
    bottomRight.updateLeftEdge(e20);
    edges.add(e20);

    Edge e21 = new Edge(bottomMiddle, new MtCell(), 0);
    bottomMiddle.updateDownEdge(e21);

    Edge e22 = new Edge(bottomRight, middleRight, r.nextInt(1000));
    bottomRight.updateUpEdge(e22);
    middleRight.updateDownEdge(e22);
    edges.add(e22);

    Edge e23 = new Edge(bottomRight, new MtCell(), 0);
    bottomRight.updateRightEdge(e23);

    Edge e24 = new Edge(bottomRight, new MtCell(), 0);
    bottomRight.updateDownEdge(e24);
  }

  void testUtilsCreateEdges(Tester t) {
    this.init();
    this.initEdges();

    // all of the work for this is done above, in init edges!
    new Utils().createEdges(cells, 100, 1, 1);

    // creating a copy of the cells
    ArrayList<ArrayList<Cell>> copyCells = new ArrayList<>();
    copyCells.add(new ArrayList<Cell>());
    copyCells.add(new ArrayList<Cell>());
    copyCells.add(new ArrayList<Cell>());

    copyCells.get(0).add(new Cell(new Posn(0, 0)));
    copyCells.get(0).add(new Cell(new Posn(1, 0)));
    copyCells.get(0).add(new Cell(new Posn(2, 0)));

    copyCells.get(1).add(new Cell(new Posn(0, 1)));
    copyCells.get(1).add(new Cell(new Posn(1, 1)));
    copyCells.get(1).add(new Cell(new Posn(2, 1)));

    copyCells.get(2).add(new Cell(new Posn(0, 2)));
    copyCells.get(2).add(new Cell(new Posn(1, 2)));
    copyCells.get(2).add(new Cell(new Posn(2, 2)));

    // WOW THIS WORKS AHAHAHHAHA
    t.checkExpect(new Utils().createEdges(copyCells, 100, 1,
        1), edges);

  }

  void testCompareEdges(Tester t) {
    this.init();
    this.initEdges();
    // these are the first 12 numbers of rand(100)
    /*
     * 915
     * 250x
     * 874
     * 988
     * 291x
     * 666x
     * 36x
     * 288x
     * 723x
     * 713x
     * 622x
     * 717x
     */

    t.checkExpect(new CompareEdges().compare(edges.get(0), edges.get(1)), 665);
    t.checkExpect(new CompareEdges().compare(edges.get(1), edges.get(2)), -624);

    // testing sort
    ArrayList<Edge> sortedEdges = new ArrayList<>();
    sortedEdges.add(edges.get(6));
    sortedEdges.add(edges.get(1));
    sortedEdges.add(edges.get(7));
    sortedEdges.add(edges.get(4));
    sortedEdges.add(edges.get(10));
    sortedEdges.add(edges.get(5));
    sortedEdges.add(edges.get(9));
    sortedEdges.add(edges.get(11));
    sortedEdges.add(edges.get(8));
    sortedEdges.add(edges.get(2));
    sortedEdges.add(edges.get(0));
    sortedEdges.add(edges.get(3));
    edges.sort(new CompareEdges());
    t.checkExpect(sortedEdges, edges);

  }

  void testDrawGraphDrawRow(Tester t) {

    // creating a cell surrounded by edges and empty cells
    Cell cell1 = new Cell(new Posn(0, 0));
    Edge e1 = new Edge(cell1, new MtCell(), 0);
    cell1.updateUpEdge(e1);
    Edge e2 = new Edge(cell1, new MtCell(), 0);
    cell1.updateDownEdge(e2);
    Edge e3 = new Edge(cell1, new MtCell(), 0);
    cell1.updateLeftEdge(e3);
    Edge e4 = new Edge(cell1, new MtCell(), 0);
    cell1.updateRightEdge(e4);

    // simulating a 1 by 1 maze, where cell1 is both topLeft and bottomRight
    WorldScene original = new WorldScene(30, 30);
    DrawGraph g = new DrawGraph(original, 30, cell1, cell1);
    // places the image of a cell on the given worldsceneww
    g.drawCellRow(cell1, new Posn(0, 0), e3, e4, e1, e2, Color.GRAY, true, 0);

    WorldScene copy = new WorldScene(30, 30);
    WorldImage cellImg = new RectangleImage(30, 30, OutlineMode.SOLID, Color.GRAY);
    WorldImage wallImage = new RectangleImage(3, 30, OutlineMode.SOLID, Color.BLACK);
    cellImg = new OverlayOffsetAlign(AlignModeX.LEFT, AlignModeY.MIDDLE, wallImage,
        0, 0, cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.RIGHT, AlignModeY.MIDDLE, wallImage,
        0, 0, cellImg);

    cellImg = new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.TOP,
        new RotateImage(wallImage, 90),
        0, 0, cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.BOTTOM,
        new RotateImage(wallImage, 90),
        0, 0, cellImg);
    Posn placeImgPosn = new Posn(16, 16);
    copy.placeImageXY(cellImg, placeImgPosn.x, placeImgPosn.y);
    t.checkExpect(copy, original);

    // breaking the top wall
    e1.breakWall();
    original = new WorldScene(30, 30);
    g = new DrawGraph(original, 30, cell1, cell1);
    // places the image of a cell on the given worldsceneww
    g.drawCellRow(cell1, new Posn(0, 0), e3, e4, e1, e2, Color.GRAY, true, 0);

    copy = new WorldScene(30, 30);
    cellImg = new RectangleImage(30, 30, OutlineMode.SOLID, Color.GRAY);
    wallImage = new RectangleImage(3, 30, OutlineMode.SOLID, Color.BLACK);

    // left wall
    cellImg = new OverlayOffsetAlign(AlignModeX.LEFT, AlignModeY.MIDDLE, wallImage,
        0, 0, cellImg);

    // right wall
    cellImg = new OverlayOffsetAlign(AlignModeX.RIGHT, AlignModeY.MIDDLE, wallImage,
        0, 0, cellImg);

    // bottom wall
    cellImg = new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.BOTTOM,
        new RotateImage(wallImage, 90),
        0, 0, cellImg);

    copy.placeImageXY(cellImg, placeImgPosn.x, placeImgPosn.y);
    t.checkExpect(copy, original);

    // adding another cell to the right
    WorldScene twoCellScene = new WorldScene(60, 30);
    WorldScene twoCellSceneCopy = new WorldScene(60, 30);

    Cell cell2 = new Cell(new Posn(1, 0));
    e4 = new Edge(cell1, cell2, 0);
    cell1.updateRightEdge(e4);
    cell2.updateLeftEdge(e4);
    Edge e5 = new Edge(cell2, new MtCell(), 0);
    cell2.updateUpEdge(e5);
    Edge e6 = new Edge(cell2, new MtCell(), 0);
    cell2.updateDownEdge(e6);
    Edge e7 = new Edge(cell2, new MtCell(), 0);
    cell2.updateRightEdge(e7);

    WorldImage cell2Img = new RectangleImage(30, 30, OutlineMode.SOLID, Color.GRAY);

    // left wall
    cell2Img = new OverlayOffsetAlign(AlignModeX.LEFT, AlignModeY.MIDDLE, wallImage,
        0, 0, cell2Img);

    // right wall
    cell2Img = new OverlayOffsetAlign(AlignModeX.RIGHT, AlignModeY.MIDDLE, wallImage,
        0, 0, cell2Img);

    // top wall
    cell2Img = new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.TOP,
        new RotateImage(wallImage, 90),
        0, 0, cell2Img);

    // bottom wall
    cell2Img = new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.BOTTOM,
        new RotateImage(wallImage, 90),
        0, 0, cell2Img);

    twoCellSceneCopy.placeImageXY(cellImg, 16, 16);
    twoCellSceneCopy.placeImageXY(cell2Img, 46, 16);

    g = new DrawGraph(twoCellScene, 30, cell1, cell2);
    g.drawCellRow(cell1, new Posn(0, 0), e3, e4, e1, e2, Color.GRAY, false, 0);

    t.checkExpect(twoCellScene, twoCellSceneCopy);

    // testing with 6 cells
    // the bottom 6 of the cells in init
    init();
    initEdges();

    WorldScene scene = new WorldScene(120, 120);
    g = new DrawGraph(scene, 30, middleLeft, bottomRight);
    g.drawCellRow(middleLeft, new Posn(0, 1), new Edge(middleLeft,
        new MtCell(), 0), edges.get(4), edges.get(3), edges.get(8),
        Color.GRAY, true, 0);

    WorldScene sceneCopy = new WorldScene(120, 120);
    sceneCopy.placeImageXY(cell2Img, 16, 46);
    sceneCopy.placeImageXY(cell2Img, 46, 46);
    sceneCopy.placeImageXY(cell2Img, 76, 46);
    sceneCopy.placeImageXY(cell2Img, 16, 76);
    sceneCopy.placeImageXY(cell2Img, 46, 76);
    sceneCopy.placeImageXY(cell2Img, 76, 76);

    t.checkExpect(scene, sceneCopy);
  }

  // testing draw cell, which literally does exactly what DrawGraph drawRow does
  void testDrawCell(Tester t) {
    init();
    initEdges();
    WorldImage cellImg = new RectangleImage(30, 30, OutlineMode.SOLID, Color.GRAY);
    WorldImage wallImage = new RectangleImage(3, 30, OutlineMode.SOLID, Color.BLACK);

    // left wall
    cellImg = new OverlayOffsetAlign(AlignModeX.LEFT, AlignModeY.MIDDLE, wallImage,
        0, 0, cellImg);

    // right wall
    cellImg = new OverlayOffsetAlign(AlignModeX.RIGHT, AlignModeY.MIDDLE, wallImage,
        0, 0, cellImg);

    // top wall
    cellImg = new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.TOP,
        new RotateImage(wallImage, 90),
        0, 0, cellImg);

    // bottom wall
    cellImg = new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.BOTTOM,
        new RotateImage(wallImage, 90),
        0, 0, cellImg);

    WorldScene scene = new WorldScene(90, 90);
    DrawGraph g = new DrawGraph(scene, 30, topLeft, bottomRight);
    // firstcall is true, so this will draw the row below topLeft as well
    topLeft.draw(g, true, 0, 0);

    WorldScene sceneCopy = new WorldScene(90, 90);

    // row 1
    sceneCopy.placeImageXY(cellImg, 16, 16);
    sceneCopy.placeImageXY(cellImg, 46, 16);
    sceneCopy.placeImageXY(cellImg, 76, 16);

    // row 2
    sceneCopy.placeImageXY(cellImg, 16, 46);
    sceneCopy.placeImageXY(cellImg, 46, 46);
    sceneCopy.placeImageXY(cellImg, 76, 46);

    // row 3
    sceneCopy.placeImageXY(cellImg, 16, 76);
    sceneCopy.placeImageXY(cellImg, 46, 76);
    sceneCopy.placeImageXY(cellImg, 76, 76);

    t.checkExpect(scene, sceneCopy);

    scene = new WorldScene(90, 90);
    g = new DrawGraph(scene, 30, topLeft, bottomRight);
    // firstcall is false, so it will only draw the row of topLeft
    topLeft.draw(g, false, 0, 0);

    sceneCopy = new WorldScene(90, 90);
    // row 1
    sceneCopy.placeImageXY(cellImg, 16, 16);
    sceneCopy.placeImageXY(cellImg, 46, 16);
    sceneCopy.placeImageXY(cellImg, 76, 16);

    t.checkExpect(scene, sceneCopy);
  }

  void testGetId(Tester t) {
    init();
    t.checkExpect(topLeft.getId(), new Posn(0, 0));
    t.checkExpect(middleLeft.getId(), new Posn(0, 1));
    t.checkExpect(bottomRight.getId(), new Posn(2, 2));
  }

  void testKruskalsUnionFind(Tester t) {
    init();
    initEdges();
    KruskalsUnionFind kuf = new KruskalsUnionFind(edges);
    // puts all of the edges in the hashmap
    kuf.putEdges();
    // testing put edges befor any iteration, every cell should be mapped to itself
    t.checkExpect(kuf.find(topLeft.getId()), topLeft.getId());
    t.checkExpect(kuf.find(topMiddle.getId()), topMiddle.getId());
    t.checkExpect(kuf.find(topRight.getId()), topRight.getId());

    // iterating kuf once
    // this makes the edge between topRight and middleright open
    kuf.iterate();
    t.checkExpect(kuf.find(middleRight.getId()), topRight.getId());
    t.checkExpect(kuf.find(topRight.getId()), topRight.getId());

    // iterating again
    // this makes the edge between topright and topMiddle open
    kuf.iterate();
    t.checkExpect(kuf.find(middleRight.getId()), topRight.getId());
    t.checkExpect(kuf.find(topMiddle.getId()), topRight.getId());
    t.checkExpect(kuf.find(topRight.getId()), topRight.getId());

    // iterating again
    // this makes the edge between middle left and bottom middle open
    kuf.iterate();
    t.checkExpect(kuf.find(bottomLeft.getId()), middleLeft.getId());
    t.checkExpect(kuf.find(middleLeft.getId()), middleLeft.getId());

    // makes the edge btween middle middle and topmiddle open
    kuf.iterate();
    t.checkExpect(kuf.find(middleMiddle.getId()), topRight.getId());
    t.checkExpect(kuf.find(topRight.getId()), topRight.getId());

    // makes the edge between bottom middle and bottom right
    kuf.iterate();
    t.checkExpect(kuf.find(bottomMiddle.getId()), bottomRight.getId());
    t.checkExpect(kuf.find(bottomRight.getId()), bottomRight.getId());

    // tries to remove the edge between middle middle and middle left,
    // but that would create a cycle
    kuf.iterate();
    t.checkExpect(kuf.find(middleMiddle.getId()), topRight.getId());
    t.checkExpect(kuf.find(middleRight.getId()), topRight.getId());

    // removes the edge between bottom middle and middle middle
    kuf.iterate();
    t.checkExpect(kuf.find(bottomMiddle.getId()), topRight.getId());
    t.checkExpect(kuf.find(middleMiddle.getId()), topRight.getId());

    // removes the edge between bottom right and middle right
    kuf.iterate();
    t.checkExpect(kuf.isFinished(), false);
    t.checkExpect(kuf.find(bottomRight.getId()), topRight.getId());
    t.checkExpect(kuf.find(middleRight.getId()), topRight.getId());

    // removes the edge between bottom left and bottom middle
    kuf.iterate();
    t.checkExpect(kuf.isFinished(), false);
    t.checkExpect(kuf.find(bottomLeft.getId()), topRight.getId());
    t.checkExpect(kuf.find(bottomMiddle.getId()), topRight.getId());

    // removes the edge between topLeft and middleLeft
    kuf.iterate();
    t.checkExpect(kuf.isFinished(), false);

    t.checkExpect(kuf.find(topLeft.getId()), topLeft.getId());
    t.checkExpect(kuf.find(middleLeft.getId()), topLeft.getId());
    t.checkExpect(kuf.find(topRight.getId()), topLeft.getId());
    t.checkExpect(kuf.find(bottomLeft.getId()), topLeft.getId());
    t.checkExpect(kuf.find(bottomMiddle.getId()), topLeft.getId());
    t.checkExpect(kuf.find(bottomRight.getId()), topLeft.getId());
    t.checkExpect(kuf.find(topMiddle.getId()), topLeft.getId());
    t.checkExpect(kuf.find(middleMiddle.getId()), topLeft.getId());
    t.checkExpect(kuf.find(middleRight.getId()), topLeft.getId());

    // removing the last 2 does nothing
    kuf.iterate();
    kuf.iterate();
    t.checkExpect(kuf.find(topLeft.getId()), topLeft.getId());
    t.checkExpect(kuf.find(middleLeft.getId()), topLeft.getId());
    t.checkExpect(kuf.find(topRight.getId()), topLeft.getId());
    t.checkExpect(kuf.find(bottomLeft.getId()), topLeft.getId());
    t.checkExpect(kuf.find(bottomMiddle.getId()), topLeft.getId());
    t.checkExpect(kuf.find(bottomRight.getId()), topLeft.getId());
    t.checkExpect(kuf.find(topMiddle.getId()), topLeft.getId());
    t.checkExpect(kuf.find(middleMiddle.getId()), topLeft.getId());
    t.checkExpect(kuf.find(middleRight.getId()), topLeft.getId());

    // should now be finished, as there are no edges left
    t.checkExpect(kuf.isFinished(), true);

    this.init();
    this.initEdges();
    this.edges.sort(new CompareEdges());
    // testing connectCells, which tries to connect the two cells of an edge
    kuf = new KruskalsUnionFind(edges);
    // puts all of the edges in the hashmap
    kuf.putEdges();
    // testing find befor any iteration, every cell should be mapped to itself
    t.checkExpect(kuf.find(topLeft.getId()), topLeft.getId());
    t.checkExpect(kuf.find(topMiddle.getId()), topMiddle.getId());
    t.checkExpect(kuf.find(topRight.getId()), topRight.getId());

    // testing connect cells on the ege between topRight and middleRight
    // this makes the edge between topRight and middleright open
    kuf.connectCells(edges.get(0), middleRight, topRight);
    t.checkExpect(kuf.find(middleRight.getId()), topRight.getId());
    t.checkExpect(kuf.find(topRight.getId()), topRight.getId());

    // iterating again
    // this makes the edge between topright and topMiddle open
    kuf.connectCells(edges.get(1), topMiddle, topRight);
    t.checkExpect(kuf.find(middleRight.getId()), topRight.getId());
    t.checkExpect(kuf.find(topMiddle.getId()), topRight.getId());
    t.checkExpect(kuf.find(topRight.getId()), topRight.getId());

    // iterating again
    // this makes the edge between middle left and bottom left open
    kuf.connectCells(edges.get(2), bottomLeft, middleLeft);
    t.checkExpect(kuf.find(bottomLeft.getId()), middleLeft.getId());
    t.checkExpect(kuf.find(middleLeft.getId()), middleLeft.getId());

    init();
    initEdges();
    // testing union
    kuf = new KruskalsUnionFind(edges);
    kuf.putEdges();
    kuf.union(new Posn(0, 0), new Posn(1, 0));
    t.checkExpect(kuf.find(topLeft.getId()), topMiddle.getId());
    t.checkExpect(kuf.find(topMiddle.getId()), topMiddle.getId());

    kuf.union(bottomRight.getId(), bottomMiddle.getId());
    t.checkExpect(kuf.find(bottomRight.getId()), bottomMiddle.getId());
  }

  // testing search helper in the depth first mode
  void testSearchHelperDFS(Tester t) {
    init();
    initEdges();

    ICollection<Cell> stack = new Stack<>();
    SearchHelper sh = new SearchHelper(topLeft, bottomRight, stack);
    // connecting the cells, so the search works
    KruskalsUnionFind kuf = new KruskalsUnionFind(edges);
    while (!kuf.isFinished()) {
      kuf.iterate();
    }
    // finished should be false, because the topLeft cell is in the workList
    t.checkExpect(sh.isFinished(), false);
    t.checkExpect(stack.remove(), topLeft);
    stack.add(topLeft);

    // then visits the middle left and adds it
    sh.iterate();
    t.checkExpect(stack.remove(), middleLeft);
    stack.add(middleLeft);

    // then visits bottom left and adds it
    sh.iterate();
    t.checkExpect(stack.remove(), bottomLeft);
    stack.add(bottomLeft);

    // then visits bottom middle and adds it
    sh.iterate();
    t.checkExpect(stack.remove(), bottomMiddle);
    stack.add(bottomMiddle);

    // now middle middle is at the front of the worklist
    sh.iterate();
    t.checkExpect(stack.remove(), middleMiddle);
    stack.add(middleMiddle);

    // now top middle is at the front of the worklist
    sh.iterate();
    t.checkExpect(stack.remove(), topMiddle);
    stack.add(topMiddle);

    // now top right is at the front of the worklist
    sh.iterate();
    t.checkExpect(stack.remove(), topRight);
    stack.add(topRight);

    // now middle right is at the front of the worklist
    sh.iterate();
    t.checkExpect(stack.remove(), middleRight);
    stack.add(middleRight);

    // now bottom right is FINALLy at the front of the worklist
    sh.iterate();
    t.checkExpect(stack.remove(), bottomRight);
    stack.add(bottomRight);

    // with the final iteration, bottom right is pulled and checked
    sh.iterate();
    t.checkExpect(sh.isFinished(), true);

    // to prove that this all works as intended, i will draw da maze image :(

    // image for topLeft, WHICH IS HIGHLIGHTED
    // highlighted - new Color(3, 252, 198)
    // visited - new Color(50, 168, 131)

    WorldScene scene = new WorldScene(90, 90);

    // top left img
    WorldImage cellImg = new RectangleImage(30, 30, OutlineMode.SOLID,
        new Color(3, 252, 198));
    cellImg = new OverlayOffsetAlign(AlignModeX.LEFT, AlignModeY.MIDDLE,
        new RectangleImage(3, 30,
            OutlineMode.SOLID,
            Color.BLACK),
        0, 0, cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.RIGHT, AlignModeY.MIDDLE,
        new RectangleImage(3, 30,
            OutlineMode.SOLID,
            Color.BLACK),
        0, 0, cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.TOP,
        new RotateImage(new RectangleImage(3, 30,
            OutlineMode.SOLID,
            Color.BLACK), 90),
        0, 0, cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.BOTTOM,
        new RotateImage(new EmptyImage(), 90), 0, 0,
        cellImg);
    scene.placeImageXY(cellImg, 16, 16);

    // top middle img
    cellImg = new RectangleImage(30, 30, OutlineMode.SOLID, new Color(50, 168, 129));
    cellImg = new OverlayOffsetAlign(AlignModeX.LEFT, AlignModeY.MIDDLE, new RectangleImage(3, 30,
        OutlineMode.SOLID,
        Color.BLACK), 0, 0, cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.RIGHT, AlignModeY.MIDDLE,
        new EmptyImage(), 0, 0, cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.TOP,
        new RotateImage(new RectangleImage(3, 30,
            OutlineMode.SOLID,
            Color.BLACK), 90),
        0, 0, cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.BOTTOM,
        new RotateImage(new EmptyImage(), 90), 0, 0,
        cellImg);
    scene.placeImageXY(cellImg, 46, 16);

    // top right img
    cellImg = new RectangleImage(30, 30, OutlineMode.SOLID, new Color(50, 168, 129));
    cellImg = new OverlayOffsetAlign(AlignModeX.LEFT, AlignModeY.MIDDLE,
        new EmptyImage(), 0, 0, cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.RIGHT, AlignModeY.MIDDLE,
        new RectangleImage(3, 30,
            OutlineMode.SOLID,
            Color.BLACK),
        0, 0, cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.TOP,
        new RotateImage(new RectangleImage(3, 30,
            OutlineMode.SOLID,
            Color.BLACK), 90),
        0, 0, cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.BOTTOM,
        new RotateImage(new EmptyImage(), 90), 0, 0,
        cellImg);
    scene.placeImageXY(cellImg, 76, 16);

    // middle left img
    cellImg = new RectangleImage(30, 30, OutlineMode.SOLID, new Color(3, 252, 198));
    cellImg = new OverlayOffsetAlign(AlignModeX.LEFT, AlignModeY.MIDDLE,
        new RectangleImage(3, 30,
            OutlineMode.SOLID,
            Color.BLACK),
        0, 0, cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.RIGHT, AlignModeY.MIDDLE,
        new RectangleImage(3, 30,
            OutlineMode.SOLID,
            Color.BLACK),
        0, 0, cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.TOP,
        new RotateImage(new EmptyImage(), 90), 0, 0,
        cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.BOTTOM,
        new RotateImage(new EmptyImage(), 90), 0, 0,
        cellImg);
    scene.placeImageXY(cellImg, 16, 46);

    // middle middle img
    cellImg = new RectangleImage(30, 30, OutlineMode.SOLID, new Color(50, 168, 129));
    cellImg = new OverlayOffsetAlign(AlignModeX.LEFT, AlignModeY.MIDDLE,
        new RectangleImage(3, 30,
            OutlineMode.SOLID,
            Color.BLACK),
        0, 0, cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.RIGHT, AlignModeY.MIDDLE,
        new RectangleImage(3, 30,
            OutlineMode.SOLID,
            Color.BLACK),
        0, 0, cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.TOP,
        new RotateImage(new EmptyImage(), 90), 0, 0,
        cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.BOTTOM,
        new RotateImage(new EmptyImage(), 90), 0, 0,
        cellImg);
    scene.placeImageXY(cellImg, 46, 46);

    // middle right img
    cellImg = new RectangleImage(30, 30, OutlineMode.SOLID, new Color(50, 168, 129));
    cellImg = new OverlayOffsetAlign(AlignModeX.LEFT, AlignModeY.MIDDLE,
        new RectangleImage(3, 30,
            OutlineMode.SOLID,
            Color.BLACK),
        0, 0, cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.RIGHT, AlignModeY.MIDDLE,
        new RectangleImage(3, 30,
            OutlineMode.SOLID,
            Color.BLACK),
        0, 0, cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.TOP,
        new RotateImage(new EmptyImage(), 90), 0, 0,
        cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.BOTTOM,
        new RotateImage(new RectangleImage(3, 30,
            OutlineMode.SOLID,
            Color.BLACK), 90),
        0, 0, cellImg);
    scene.placeImageXY(cellImg, 76, 46);

    // bottom left img
    cellImg = new RectangleImage(30, 30, OutlineMode.SOLID, new Color(3, 252, 198));
    cellImg = new OverlayOffsetAlign(AlignModeX.LEFT, AlignModeY.MIDDLE,
        new RectangleImage(3, 30,
            OutlineMode.SOLID,
            Color.BLACK),
        0, 0, cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.RIGHT, AlignModeY.MIDDLE,
        new EmptyImage(), 0, 0, cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.TOP,
        new RotateImage(new EmptyImage(), 90), 0, 0,
        cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.BOTTOM,
        new RotateImage(new RectangleImage(3, 30,
            OutlineMode.SOLID,
            Color.BLACK), 90),
        0, 0, cellImg);
    scene.placeImageXY(cellImg, 16, 76);

    // bottom middle img
    cellImg = new RectangleImage(30, 30, OutlineMode.SOLID, new Color(3, 252, 198));
    cellImg = new OverlayOffsetAlign(AlignModeX.LEFT, AlignModeY.MIDDLE,
        new EmptyImage(), 0, 0, cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.RIGHT, AlignModeY.MIDDLE,
        new EmptyImage(), 0, 0, cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.TOP,
        new RotateImage(new EmptyImage(), 90), 0, 0,
        cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.BOTTOM,
        new RotateImage(new RectangleImage(3, 30,
            OutlineMode.SOLID,
            Color.BLACK), 90),
        0, 0, cellImg);
    scene.placeImageXY(cellImg, 46, 76);

    // bottom right img
    cellImg = new RectangleImage(30, 30, OutlineMode.SOLID, new Color(3, 252, 198));
    cellImg = new OverlayOffsetAlign(AlignModeX.LEFT, AlignModeY.MIDDLE,
        new EmptyImage(), 0, 0, cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.RIGHT, AlignModeY.MIDDLE,
        new RectangleImage(3, 30,
            OutlineMode.SOLID,
            Color.BLACK),
        0, 0, cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.TOP,
        new RotateImage(new RectangleImage(3, 30,
            OutlineMode.SOLID,
            Color.BLACK), 90),
        0, 0, cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.BOTTOM,
        new RotateImage(new RectangleImage(3, 30,
            OutlineMode.SOLID,
            Color.BLACK), 90),
        0, 0, cellImg);
    scene.placeImageXY(cellImg, 76, 76);

    WorldScene copyScene = new WorldScene(90, 90);
    DrawGraph g = new DrawGraph(copyScene, 30, topLeft, bottomRight);
    topLeft.draw(g, true, 0, 0);

    t.checkExpect(scene, copyScene);
  }

  // testing the bfs mode of SearchHelper
  void testSearchHelperBFS(Tester t) {
    init();
    initEdges();

    ICollection<Cell> queue = new Queue<>();
    SearchHelper sh = new SearchHelper(topLeft, bottomRight, queue);
    // connecting the cells, so the search works
    KruskalsUnionFind kuf = new KruskalsUnionFind(edges);
    while (!kuf.isFinished()) {
      kuf.iterate();
    }
    // finished should be false, because the topLeft cell is in the workList
    t.checkExpect(sh.isFinished(), false);
    t.checkExpect(queue.remove(), topLeft);
    // have to restart because now the order of the queue is messed up
    queue = new Queue<>();
    this.init();
    this.initEdges();
    kuf = new KruskalsUnionFind(edges);
    while (!kuf.isFinished()) {
      kuf.iterate();
    }
    sh = new SearchHelper(topLeft, bottomRight, queue);

    // then visits the middle left and adds it
    sh.iterate();
    t.checkExpect(queue.remove(), middleLeft);
    // have to restart because now the order of the queue is messed up
    queue = new Queue<>();
    this.init();
    this.initEdges();
    kuf = new KruskalsUnionFind(edges);
    while (!kuf.isFinished()) {
      kuf.iterate();
    }
    sh = new SearchHelper(topLeft, bottomRight, queue);
    sh.iterate();

    // then visits bottom left and adds it
    sh.iterate();
    t.checkExpect(queue.remove(), bottomLeft);
    // have to restart because now the order of the queue is messed up
    queue = new Queue<>();
    this.init();
    this.initEdges();
    kuf = new KruskalsUnionFind(edges);
    while (!kuf.isFinished()) {
      kuf.iterate();
    }
    sh = new SearchHelper(topLeft, bottomRight, queue);
    sh.iterate();
    sh.iterate();

    // then visits bottom middle and adds it
    sh.iterate();
    t.checkExpect(queue.remove(), bottomMiddle);
    // have to restart because now the order of the queue is messed up
    queue = new Queue<>();
    this.init();
    this.initEdges();
    kuf = new KruskalsUnionFind(edges);
    while (!kuf.isFinished()) {
      kuf.iterate();
    }
    sh = new SearchHelper(topLeft, bottomRight, queue);
    sh.iterate();
    sh.iterate();
    sh.iterate();

    // then, puts bottom middle's neighbros in the queue, middle middle and
    // bottom left
    sh.iterate();
    t.checkExpect(queue.remove(), bottomRight);
    // have to restart because now the order of the queue is messed up
    queue = new Queue<>();
    this.init();
    this.initEdges();
    kuf = new KruskalsUnionFind(edges);
    while (!kuf.isFinished()) {
      kuf.iterate();
    }
    sh = new SearchHelper(topLeft, bottomRight, queue);
    sh.iterate();
    sh.iterate();
    sh.iterate();
    sh.iterate();

    // iterates again, and finds bottom right, so finishes
    sh.iterate();

    t.checkExpect(sh.isFinished(), true);

    // testing the image NOW
    // AHAHAHHAHA
    WorldScene scene = new WorldScene(90, 90);

    // top left img
    WorldImage cellImg = new RectangleImage(30, 30, OutlineMode.SOLID,
        new Color(3, 252, 198));
    cellImg = new OverlayOffsetAlign(AlignModeX.LEFT, AlignModeY.MIDDLE,
        new RectangleImage(3, 30,
            OutlineMode.SOLID,
            Color.BLACK),
        0, 0, cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.RIGHT, AlignModeY.MIDDLE,
        new RectangleImage(3, 30,
            OutlineMode.SOLID,
            Color.BLACK),
        0, 0, cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.TOP,
        new RotateImage(new RectangleImage(3, 30,
            OutlineMode.SOLID,
            Color.BLACK), 90),
        0, 0, cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.BOTTOM,
        new RotateImage(new EmptyImage(), 90), 0, 0,
        cellImg);
    scene.placeImageXY(cellImg, 16, 16);

    // top middle img was not visited
    cellImg = new RectangleImage(30, 30, OutlineMode.SOLID, Color.GRAY);
    cellImg = new OverlayOffsetAlign(AlignModeX.LEFT, AlignModeY.MIDDLE,
        new RectangleImage(3, 30,
            OutlineMode.SOLID,
            Color.BLACK),
        0, 0, cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.RIGHT, AlignModeY.MIDDLE,
        new EmptyImage(), 0, 0, cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.TOP,
        new RotateImage(new RectangleImage(3, 30,
            OutlineMode.SOLID,
            Color.BLACK), 90),
        0, 0, cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.BOTTOM,
        new RotateImage(new EmptyImage(), 90), 0, 0,
        cellImg);
    scene.placeImageXY(cellImg, 46, 16);

    // top right img
    cellImg = new RectangleImage(30, 30, OutlineMode.SOLID, Color.GRAY);
    cellImg = new OverlayOffsetAlign(AlignModeX.LEFT, AlignModeY.MIDDLE,
        new EmptyImage(), 0, 0, cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.RIGHT, AlignModeY.MIDDLE,
        new RectangleImage(3, 30,
            OutlineMode.SOLID,
            Color.BLACK),
        0, 0, cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.TOP,
        new RotateImage(new RectangleImage(3, 30,
            OutlineMode.SOLID,
            Color.BLACK), 90),
        0, 0, cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.BOTTOM,
        new RotateImage(new EmptyImage(), 90), 0, 0,
        cellImg);
    scene.placeImageXY(cellImg, 76, 16);

    // middle left img
    cellImg = new RectangleImage(30, 30, OutlineMode.SOLID, new Color(3, 252, 198));
    cellImg = new OverlayOffsetAlign(AlignModeX.LEFT, AlignModeY.MIDDLE,
        new RectangleImage(3, 30,
            OutlineMode.SOLID,
            Color.BLACK),
        0, 0, cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.RIGHT, AlignModeY.MIDDLE,
        new RectangleImage(3, 30,
            OutlineMode.SOLID,
            Color.BLACK),
        0, 0, cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.TOP,
        new RotateImage(new EmptyImage(), 90), 0, 0,
        cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.BOTTOM,
        new RotateImage(new EmptyImage(), 90), 0, 0,
        cellImg);
    scene.placeImageXY(cellImg, 16, 46);

    // middle middle img
    cellImg = new RectangleImage(30, 30, OutlineMode.SOLID, Color.GRAY);
    cellImg = new OverlayOffsetAlign(AlignModeX.LEFT, AlignModeY.MIDDLE,
        new RectangleImage(3, 30,
            OutlineMode.SOLID,
            Color.BLACK),
        0, 0, cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.RIGHT, AlignModeY.MIDDLE,
        new RectangleImage(3, 30,
            OutlineMode.SOLID,
            Color.BLACK),
        0, 0, cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.TOP,
        new RotateImage(new EmptyImage(), 90), 0, 0,
        cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.BOTTOM,
        new RotateImage(new EmptyImage(), 90), 0, 0,
        cellImg);
    scene.placeImageXY(cellImg, 46, 46);

    // middle right img
    cellImg = new RectangleImage(30, 30, OutlineMode.SOLID, Color.GRAY);
    cellImg = new OverlayOffsetAlign(AlignModeX.LEFT, AlignModeY.MIDDLE,
        new RectangleImage(3, 30,
            OutlineMode.SOLID,
            Color.BLACK),
        0, 0, cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.RIGHT, AlignModeY.MIDDLE,
        new RectangleImage(3, 30,
            OutlineMode.SOLID,
            Color.BLACK),
        0, 0, cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.TOP,
        new RotateImage(new EmptyImage(), 90), 0, 0,
        cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.BOTTOM,
        new RotateImage(new RectangleImage(3, 30,
            OutlineMode.SOLID,
            Color.BLACK), 90),
        0, 0, cellImg);
    scene.placeImageXY(cellImg, 76, 46);

    // bottom left img
    cellImg = new RectangleImage(30, 30, OutlineMode.SOLID,
        new Color(3, 252, 198));
    cellImg = new OverlayOffsetAlign(AlignModeX.LEFT, AlignModeY.MIDDLE,
        new RectangleImage(3, 30,
            OutlineMode.SOLID,
            Color.BLACK),
        0, 0, cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.RIGHT, AlignModeY.MIDDLE,
        new EmptyImage(), 0, 0, cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.TOP,
        new RotateImage(new EmptyImage(), 90), 0, 0,
        cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.BOTTOM,
        new RotateImage(new RectangleImage(3, 30,
            OutlineMode.SOLID,
            Color.BLACK), 90),
        0, 0, cellImg);
    scene.placeImageXY(cellImg, 16, 76);

    // bottom middle img
    cellImg = new RectangleImage(30, 30, OutlineMode.SOLID, new Color(3, 252, 198));
    cellImg = new OverlayOffsetAlign(AlignModeX.LEFT, AlignModeY.MIDDLE,
        new EmptyImage(), 0, 0, cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.RIGHT, AlignModeY.MIDDLE,
        new EmptyImage(), 0, 0, cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.TOP,
        new RotateImage(new EmptyImage(), 90), 0, 0,
        cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.BOTTOM,
        new RotateImage(new RectangleImage(3, 30,
            OutlineMode.SOLID,
            Color.BLACK), 90),
        0, 0, cellImg);
    scene.placeImageXY(cellImg, 46, 76);

    // bottom right img
    cellImg = new RectangleImage(30, 30, OutlineMode.SOLID, new Color(3, 252, 198));
    cellImg = new OverlayOffsetAlign(AlignModeX.LEFT, AlignModeY.MIDDLE,
        new EmptyImage(), 0, 0, cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.RIGHT, AlignModeY.MIDDLE,
        new RectangleImage(3, 30,
            OutlineMode.SOLID,
            Color.BLACK),
        0, 0, cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.TOP,
        new RotateImage(new RectangleImage(3, 30,
            OutlineMode.SOLID,
            Color.BLACK), 90),
        0, 0, cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.BOTTOM,
        new RotateImage(new RectangleImage(3, 30,
            OutlineMode.SOLID,
            Color.BLACK), 90),
        0, 0, cellImg);
    scene.placeImageXY(cellImg, 76, 76);

    WorldScene copyScene = new WorldScene(90, 90);
    DrawGraph g = new DrawGraph(copyScene, 30, topLeft, bottomRight);
    topLeft.draw(g, true, 0, 0);

    t.checkExpect(scene, copyScene);
  }

  void testAddToWorklist(Tester t) {
    init();
    initEdges();

    // creating the maze
    KruskalsUnionFind kuf = new KruskalsUnionFind(edges);
    while (!kuf.isFinished()) {
      kuf.iterate();
    }

    ICollection<Cell> cells = new Stack<>();
    // adding topLeft to worklist
    topLeft.addToList(cells);
    t.checkExpect(cells.remove(), topLeft);
    // adding bottom right to worklist
    bottomRight.addToList(cells);
    t.checkExpect(cells.remove(), bottomRight);
    // adding empty
    new MtCell().addToList(cells);
    t.checkExpect(cells, new Stack<>());

    // adding neighbors to worklist
    topLeft.addNeighborsToWorklist(cells);
    t.checkExpect(cells.remove(), middleLeft);

    // adding topLeft to worklist is now impossible, since topLeft has now
    // been visited
    topLeft.addToList(cells);
    t.checkExpect(cells, new Stack<>());

    middleLeft.addNeighborsToWorklist(cells);
    t.checkExpect(cells.remove(), bottomLeft);

    middleMiddle.addNeighborsToWorklist(cells);
    t.checkExpect(cells.remove(), bottomMiddle);
    t.checkExpect(cells.remove(), topMiddle);

    // restarting
    init();
    initEdges();
    // creating the maze
    kuf = new KruskalsUnionFind(edges);
    while (!kuf.isFinished()) {
      kuf.iterate();
    }
    cells = new Stack<>();

    // trying to add topLeft's right neigbhor using
    // edge addNeighbor, which doesn't do anything, because this
    // edge is walled

    edges.get(10).addNeighborToWorklist(cells, topLeft);
    t.checkExpect(cells, new Stack<>());

    // doing the same with topLeft's bottom edge, which isn't walled
    edges.get(9).addNeighborToWorklist(cells, topLeft);
    t.checkExpect(cells.remove(), middleLeft);

    // trying it on topLefts upper edge, which is an edge with an mtCell
    Edge emptyEdge = new Edge(topLeft, new MtCell(), 0);
    topLeft.updateLeftEdge(emptyEdge);
    emptyEdge.addNeighborToWorklist(cells, topLeft);
    t.checkExpect(cells, new Stack<>());

  }

  // testing the manual movement methods
  void testMove(Tester t) {
    init();
    initEdges();
    KruskalsUnionFind kuf = new KruskalsUnionFind(edges);
    while (!kuf.isFinished()) {
      kuf.iterate();
    }
    Cell current = topLeft;

    // returns topLeft because the player cannot move up
    t.checkExpect(current.moveUp(), topLeft);

    // returns topLeft because the player cannot move left
    t.checkExpect(current.moveLeft(), topLeft);

    // returns topLeft because the player cannot move right
    t.checkExpect(current.moveRight(), topLeft);

    // returns middleLeft, because the player can move down
    current = current.moveDown();
    t.checkExpect(current, middleLeft);

    // returns bottomLeft, because the player can move down
    current = current.moveDown();
    t.checkExpect(current, bottomLeft);

    // returns bottomMiddle, because the player can move right
    current = current.moveRight();
    t.checkExpect(current, bottomMiddle);

    // returns middleMiddle, because the player can move up
    current = current.moveUp();
    t.checkExpect(current, middleMiddle);

    current = current.moveDown();
    t.checkExpect(current, bottomMiddle);

    current = current.moveLeft();
    t.checkExpect(current, bottomLeft);

    current = current.moveRight();
    current = current.moveRight();
    t.checkExpect(current, bottomRight);

    // testing the image NOW
    // AHAHAHHAHA
    WorldScene scene = new WorldScene(90, 90);

    // top left img
    WorldImage cellImg = new RectangleImage(30, 30, OutlineMode.SOLID,
        new Color(50, 168, 129));
    cellImg = new OverlayOffsetAlign(AlignModeX.LEFT, AlignModeY.MIDDLE,
        new RectangleImage(3, 30,
            OutlineMode.SOLID,
            Color.BLACK),
        0, 0, cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.RIGHT, AlignModeY.MIDDLE,
        new RectangleImage(3, 30,
            OutlineMode.SOLID,
            Color.BLACK),
        0, 0, cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.TOP,
        new RotateImage(new RectangleImage(3, 30,
            OutlineMode.SOLID,
            Color.BLACK), 90),
        0, 0, cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.BOTTOM,
        new RotateImage(new EmptyImage(), 90), 0, 0,
        cellImg);
    scene.placeImageXY(cellImg, 16, 16);

    // top middle img was not visited
    cellImg = new RectangleImage(30, 30, OutlineMode.SOLID, Color.GRAY);
    cellImg = new OverlayOffsetAlign(AlignModeX.LEFT, AlignModeY.MIDDLE,
        new RectangleImage(3, 30,
            OutlineMode.SOLID,
            Color.BLACK),
        0, 0, cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.RIGHT, AlignModeY.MIDDLE,
        new EmptyImage(), 0, 0, cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.TOP,
        new RotateImage(new RectangleImage(3, 30,
            OutlineMode.SOLID,
            Color.BLACK), 90),
        0, 0, cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.BOTTOM,
        new RotateImage(new EmptyImage(), 90), 0, 0,
        cellImg);
    scene.placeImageXY(cellImg, 46, 16);

    // top right img
    cellImg = new RectangleImage(30, 30, OutlineMode.SOLID, Color.GRAY);
    cellImg = new OverlayOffsetAlign(AlignModeX.LEFT, AlignModeY.MIDDLE,
        new EmptyImage(), 0, 0, cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.RIGHT, AlignModeY.MIDDLE,
        new RectangleImage(3, 30,
            OutlineMode.SOLID,
            Color.BLACK),
        0, 0, cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.TOP,
        new RotateImage(new RectangleImage(3, 30,
            OutlineMode.SOLID,
            Color.BLACK), 90),
        0, 0, cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.BOTTOM,
        new RotateImage(new EmptyImage(), 90), 0, 0,
        cellImg);
    scene.placeImageXY(cellImg, 76, 16);

    // middle left img
    cellImg = new RectangleImage(30, 30, OutlineMode.SOLID, new Color(50, 168, 129));
    cellImg = new OverlayOffsetAlign(AlignModeX.LEFT, AlignModeY.MIDDLE,
        new RectangleImage(3, 30,
            OutlineMode.SOLID,
            Color.BLACK),
        0, 0, cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.RIGHT, AlignModeY.MIDDLE,
        new RectangleImage(3, 30,
            OutlineMode.SOLID,
            Color.BLACK),
        0, 0, cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.TOP,
        new RotateImage(new EmptyImage(), 90), 0, 0,
        cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.BOTTOM,
        new RotateImage(new EmptyImage(), 90), 0, 0,
        cellImg);
    scene.placeImageXY(cellImg, 16, 46);

    // middle middle img
    cellImg = new RectangleImage(30, 30, OutlineMode.SOLID, new Color(50, 168, 129));
    cellImg = new OverlayOffsetAlign(AlignModeX.LEFT, AlignModeY.MIDDLE,
        new RectangleImage(3, 30,
            OutlineMode.SOLID,
            Color.BLACK),
        0, 0, cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.RIGHT, AlignModeY.MIDDLE,
        new RectangleImage(3, 30,
            OutlineMode.SOLID,
            Color.BLACK),
        0, 0, cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.TOP,
        new RotateImage(new EmptyImage(), 90), 0, 0,
        cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.BOTTOM,
        new RotateImage(new EmptyImage(), 90), 0, 0,
        cellImg);
    scene.placeImageXY(cellImg, 46, 46);

    // middle right img
    cellImg = new RectangleImage(30, 30, OutlineMode.SOLID, Color.GRAY);
    cellImg = new OverlayOffsetAlign(AlignModeX.LEFT, AlignModeY.MIDDLE,
        new RectangleImage(3, 30,
            OutlineMode.SOLID,
            Color.BLACK),
        0, 0, cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.RIGHT, AlignModeY.MIDDLE,
        new RectangleImage(3, 30,
            OutlineMode.SOLID,
            Color.BLACK),
        0, 0, cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.TOP,
        new RotateImage(new EmptyImage(), 90), 0, 0,
        cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.BOTTOM,
        new RotateImage(new RectangleImage(3, 30,
            OutlineMode.SOLID,
            Color.BLACK), 90),
        0, 0, cellImg);
    scene.placeImageXY(cellImg, 76, 46);

    // bottom left img
    cellImg = new RectangleImage(30, 30, OutlineMode.SOLID, new Color(50, 168, 129));
    cellImg = new OverlayOffsetAlign(AlignModeX.LEFT, AlignModeY.MIDDLE,
        new RectangleImage(3, 30,
            OutlineMode.SOLID,
            Color.BLACK),
        0, 0, cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.RIGHT, AlignModeY.MIDDLE,
        new EmptyImage(), 0, 0, cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.TOP,
        new RotateImage(new EmptyImage(), 90), 0, 0,
        cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.BOTTOM,
        new RotateImage(new RectangleImage(3, 30,
            OutlineMode.SOLID,
            Color.BLACK), 90),
        0, 0, cellImg);
    scene.placeImageXY(cellImg, 16, 76);

    // bottom middle img
    cellImg = new RectangleImage(30, 30, OutlineMode.SOLID, new Color(50, 168, 129));
    cellImg = new OverlayOffsetAlign(AlignModeX.LEFT, AlignModeY.MIDDLE,
        new EmptyImage(), 0, 0, cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.RIGHT, AlignModeY.MIDDLE,
        new EmptyImage(), 0, 0, cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.TOP,
        new RotateImage(new EmptyImage(), 90), 0, 0,
        cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.BOTTOM,
        new RotateImage(new RectangleImage(3, 30,
            OutlineMode.SOLID,
            Color.BLACK), 90),
        0, 0, cellImg);
    scene.placeImageXY(cellImg, 46, 76);

    // bottom right img
    cellImg = new RectangleImage(30, 30, OutlineMode.SOLID, new Color(3, 252, 198));
    cellImg = new OverlayOffsetAlign(AlignModeX.LEFT, AlignModeY.MIDDLE,
        new EmptyImage(), 0, 0, cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.RIGHT, AlignModeY.MIDDLE,
        new RectangleImage(3, 30,
            OutlineMode.SOLID,
            Color.BLACK),
        0, 0, cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.TOP,
        new RotateImage(new RectangleImage(3, 30,
            OutlineMode.SOLID,
            Color.BLACK), 90),
        0, 0, cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.BOTTOM,
        new RotateImage(new RectangleImage(3, 30,
            OutlineMode.SOLID,
            Color.BLACK), 90),
        0, 0, cellImg);
    scene.placeImageXY(cellImg, 76, 76);

    WorldScene copyScene = new WorldScene(90, 90);
    DrawGraph g = new DrawGraph(copyScene, 30, topLeft, bottomRight);
    topLeft.draw(g, true, 0, 0);

    // testing move inside of graph
    init();
    initEdges();
    kuf = new KruskalsUnionFind(edges);
    while (!kuf.isFinished()) {
      kuf.iterate();
    }
    this.graph.startManualMode();
    this.graph.skipAnimation();

    this.graph.startManualMode();
    this.graph.skipAnimation();

    this.topLeft.highlight();
    scene = new WorldScene(750, 750);
    new DrawGraph(scene, 250, topLeft, bottomRight).drawCells(0);
    t.checkExpect(graph.draw(0), scene);

    graph.move("down");
    topLeft.moveDown();
    scene = new WorldScene(750, 750);
    new DrawGraph(scene, 250, topLeft, bottomRight).drawCells(0);
    t.checkExpect(graph.draw(0), scene);

    graph.move("right");
    middleLeft.moveRight();
    scene = new WorldScene(750, 750);
    new DrawGraph(scene, 250, topLeft, bottomRight).drawCells(0);
    t.checkExpect(graph.draw(0), scene);

    graph.move("left");
    middleLeft.moveLeft();
    scene = new WorldScene(750, 750);
    new DrawGraph(scene, 250, topLeft, bottomRight).drawCells(0);
    t.checkExpect(graph.draw(0), scene);

    graph.move("up");
    middleLeft.moveUp();
    scene = new WorldScene(750, 750);
    new DrawGraph(scene, 250, topLeft, bottomRight).drawCells(0);
    t.checkExpect(graph.draw(0), scene);

    graph.move("down");
    topLeft.moveDown();
    graph.move("down");
    middleLeft.moveDown();
    scene = new WorldScene(750, 750);
    new DrawGraph(scene, 250, topLeft, bottomRight).drawCells(0);
    t.checkExpect(graph.draw(0), scene);

    graph.move("dfasfdsaf");
    scene = new WorldScene(750, 750);
    new DrawGraph(scene, 250, topLeft, bottomRight).drawCells(0);
    t.checkExpect(graph.draw(0), scene);
  }

  // testing calculate distance from this cell
  void testCalculateDistance(Tester t) {
    init();
    initEdges();
    KruskalsUnionFind kuf = new KruskalsUnionFind(edges);
    while (!kuf.isFinished()) {
      kuf.iterate();
    }

    // furthest is middle right
    t.checkExpect(topLeft.calculateDistanceFromThisCell(), 8);

    // furthest is middle right
    t.checkExpect(bottomRight.calculateDistanceFromThisCell(), 6);

    // testing update neigbor distance,
    // which returns the max distance from the current cell from all 4 edges except
    // for the cameFrom edge

    // using the edge between topLeft and middleLEft
    // this means the method has nowhere to go, since the rest of the edges are
    // blocked off
    t.checkExpect(topLeft.updateNeighborDistance(edges.get(9), 0), 1);

    // returns 8, for middleRight
    t.checkExpect(topLeft.updateNeighborDistance(null, 0), 8);

    // trying on bottomMiddle, with the edge between bottom middle and bottom right
    // includes the 3 cells between bottom middle and top left
    t.checkExpect(bottomMiddle.updateNeighborDistance(edges.get(8), 3), 8);

    // doesn't include the previous 3 cells
    t.checkExpect(bottomMiddle.updateNeighborDistance(edges.get(8), 0), 5);

    // testing on bottomRight
    t.checkExpect(bottomRight.updateNeighborDistance(null, 0), 6);
  }

  // testing updateCellHistory to create a path between topLeft and bottomRight
  void testUpdateCellHistoryAndHighlight(Tester t) {
    init();
    initEdges();
    KruskalsUnionFind kuf = new KruskalsUnionFind(edges);
    while (!kuf.isFinished()) {
      kuf.iterate();
    }

    middleLeft.updateCellHistory(topLeft);
    bottomLeft.updateCellHistory(middleLeft);
    bottomMiddle.updateCellHistory(bottomLeft);
    bottomRight.updateCellHistory(bottomMiddle);
    bottomRight.highlight();

    // testing the image NOW
    // AHAHAHHAHA
    WorldScene scene = new WorldScene(90, 90);

    // top left img
    WorldImage cellImg = new RectangleImage(30, 30, OutlineMode.SOLID,
        new Color(3, 252, 198));
    cellImg = new OverlayOffsetAlign(AlignModeX.LEFT, AlignModeY.MIDDLE,
        new RectangleImage(3, 30,
            OutlineMode.SOLID,
            Color.BLACK),
        0, 0, cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.RIGHT, AlignModeY.MIDDLE,
        new RectangleImage(3, 30,
            OutlineMode.SOLID,
            Color.BLACK),
        0, 0, cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.TOP,
        new RotateImage(new RectangleImage(3, 30,
            OutlineMode.SOLID,
            Color.BLACK), 90),
        0, 0, cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.BOTTOM,
        new RotateImage(new EmptyImage(), 90), 0, 0,
        cellImg);
    scene.placeImageXY(cellImg, 16, 16);

    // top middle img was not visited
    cellImg = new RectangleImage(30, 30, OutlineMode.SOLID, Color.GRAY);
    cellImg = new OverlayOffsetAlign(AlignModeX.LEFT, AlignModeY.MIDDLE,
        new RectangleImage(3, 30,
            OutlineMode.SOLID,
            Color.BLACK),
        0, 0, cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.RIGHT, AlignModeY.MIDDLE,
        new EmptyImage(), 0, 0, cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.TOP,
        new RotateImage(new RectangleImage(3, 30,
            OutlineMode.SOLID,
            Color.BLACK), 90),
        0, 0, cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.BOTTOM,
        new RotateImage(new EmptyImage(), 90), 0, 0,
        cellImg);
    scene.placeImageXY(cellImg, 46, 16);

    // top right img
    cellImg = new RectangleImage(30, 30, OutlineMode.SOLID, Color.GRAY);
    cellImg = new OverlayOffsetAlign(AlignModeX.LEFT, AlignModeY.MIDDLE,
        new EmptyImage(), 0, 0, cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.RIGHT, AlignModeY.MIDDLE,
        new RectangleImage(3, 30,
            OutlineMode.SOLID,
            Color.BLACK),
        0, 0, cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.TOP,
        new RotateImage(new RectangleImage(3, 30,
            OutlineMode.SOLID,
            Color.BLACK), 90),
        0, 0, cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.BOTTOM,
        new RotateImage(new EmptyImage(), 90), 0, 0,
        cellImg);
    scene.placeImageXY(cellImg, 76, 16);

    // middle left img
    cellImg = new RectangleImage(30, 30, OutlineMode.SOLID, new Color(3, 252, 198));
    cellImg = new OverlayOffsetAlign(AlignModeX.LEFT, AlignModeY.MIDDLE,
        new RectangleImage(3, 30,
            OutlineMode.SOLID,
            Color.BLACK),
        0, 0, cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.RIGHT, AlignModeY.MIDDLE,
        new RectangleImage(3, 30,
            OutlineMode.SOLID,
            Color.BLACK),
        0, 0, cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.TOP,
        new RotateImage(new EmptyImage(), 90), 0, 0,
        cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.BOTTOM,
        new RotateImage(new EmptyImage(), 90), 0, 0,
        cellImg);
    scene.placeImageXY(cellImg, 16, 46);

    // middle middle img
    cellImg = new RectangleImage(30, 30, OutlineMode.SOLID, Color.GRAY);
    cellImg = new OverlayOffsetAlign(AlignModeX.LEFT, AlignModeY.MIDDLE,
        new RectangleImage(3, 30,
            OutlineMode.SOLID,
            Color.BLACK),
        0, 0, cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.RIGHT, AlignModeY.MIDDLE,
        new RectangleImage(3, 30,
            OutlineMode.SOLID,
            Color.BLACK),
        0, 0, cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.TOP,
        new RotateImage(new EmptyImage(), 90), 0, 0,
        cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.BOTTOM,
        new RotateImage(new EmptyImage(), 90), 0, 0,
        cellImg);
    scene.placeImageXY(cellImg, 46, 46);

    // middle right img
    cellImg = new RectangleImage(30, 30, OutlineMode.SOLID, Color.GRAY);
    cellImg = new OverlayOffsetAlign(AlignModeX.LEFT, AlignModeY.MIDDLE,
        new RectangleImage(3, 30,
            OutlineMode.SOLID,
            Color.BLACK),
        0, 0, cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.RIGHT, AlignModeY.MIDDLE,
        new RectangleImage(3, 30,
            OutlineMode.SOLID,
            Color.BLACK),
        0, 0, cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.TOP,
        new RotateImage(new EmptyImage(), 90), 0, 0,
        cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.BOTTOM,
        new RotateImage(new RectangleImage(3, 30,
            OutlineMode.SOLID,
            Color.BLACK), 90),
        0, 0, cellImg);
    scene.placeImageXY(cellImg, 76, 46);

    // bottom left img
    cellImg = new RectangleImage(30, 30, OutlineMode.SOLID, new Color(3, 252, 198));
    cellImg = new OverlayOffsetAlign(AlignModeX.LEFT, AlignModeY.MIDDLE,
        new RectangleImage(3, 30,
            OutlineMode.SOLID,
            Color.BLACK),
        0, 0, cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.RIGHT, AlignModeY.MIDDLE,
        new EmptyImage(), 0, 0, cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.TOP,
        new RotateImage(new EmptyImage(), 90), 0, 0,
        cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.BOTTOM,
        new RotateImage(new RectangleImage(3, 30,
            OutlineMode.SOLID,
            Color.BLACK), 90),
        0, 0, cellImg);
    scene.placeImageXY(cellImg, 16, 76);

    // bottom middle img
    cellImg = new RectangleImage(30, 30, OutlineMode.SOLID, new Color(3, 252, 198));
    cellImg = new OverlayOffsetAlign(AlignModeX.LEFT, AlignModeY.MIDDLE,
        new EmptyImage(), 0, 0, cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.RIGHT, AlignModeY.MIDDLE,
        new EmptyImage(), 0, 0, cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.TOP,
        new RotateImage(new EmptyImage(), 90), 0, 0,
        cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.BOTTOM,
        new RotateImage(new RectangleImage(3, 30,
            OutlineMode.SOLID,
            Color.BLACK), 90),
        0, 0, cellImg);
    scene.placeImageXY(cellImg, 46, 76);

    // bottom right img
    cellImg = new RectangleImage(30, 30, OutlineMode.SOLID, new Color(3, 252, 198));
    cellImg = new OverlayOffsetAlign(AlignModeX.LEFT, AlignModeY.MIDDLE,
        new EmptyImage(), 0, 0, cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.RIGHT, AlignModeY.MIDDLE,
        new RectangleImage(3, 30,
            OutlineMode.SOLID,
            Color.BLACK),
        0, 0, cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.TOP,
        new RotateImage(new RectangleImage(3, 30,
            OutlineMode.SOLID,
            Color.BLACK), 90),
        0, 0, cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.BOTTOM,
        new RotateImage(new RectangleImage(3, 30,
            OutlineMode.SOLID,
            Color.BLACK), 90),
        0, 0, cellImg);
    scene.placeImageXY(cellImg, 76, 76);

    WorldScene copyScene = new WorldScene(90, 90);
    DrawGraph g = new DrawGraph(copyScene, 30, topLeft, bottomRight);
    topLeft.draw(g, true, 0, 0);

    t.checkExpect(scene, copyScene);
  }

  // testing drawing again now using mode 1, which creates a heatmap with topLeft
  // as the origin
  void testDrawMode1(Tester t) {
    init();
    initEdges();
    KruskalsUnionFind kuf = new KruskalsUnionFind(edges);
    while (!kuf.isFinished()) {
      kuf.iterate();
    }

    // testing the image NOW
    // AHAHAHHAHA
    WorldScene scene = new WorldScene(90, 90);

    // top left img
    WorldImage cellImg = new RectangleImage(30, 30, OutlineMode.SOLID, new Color(0, 0, 255));
    cellImg = new OverlayOffsetAlign(AlignModeX.LEFT, AlignModeY.MIDDLE,
        new RectangleImage(3, 30,
            OutlineMode.SOLID,
            Color.BLACK),
        0, 0, cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.RIGHT, AlignModeY.MIDDLE,
        new RectangleImage(3, 30,
            OutlineMode.SOLID,
            Color.BLACK),
        0, 0, cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.TOP,
        new RotateImage(new RectangleImage(3, 30,
            OutlineMode.SOLID,
            Color.BLACK), 90),
        0, 0, cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.BOTTOM,
        new RotateImage(new EmptyImage(), 90), 0, 0,
        cellImg);
    scene.placeImageXY(cellImg, 16, 16);

    // top middle img was not visited
    cellImg = new RectangleImage(30, 30, OutlineMode.SOLID, new Color(159, 0, 95));
    cellImg = new OverlayOffsetAlign(AlignModeX.LEFT, AlignModeY.MIDDLE,
        new RectangleImage(3, 30,
            OutlineMode.SOLID,
            Color.BLACK),
        0, 0, cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.RIGHT, AlignModeY.MIDDLE,
        new EmptyImage(), 0, 0, cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.TOP,
        new RotateImage(new RectangleImage(3, 30,
            OutlineMode.SOLID,
            Color.BLACK), 90),
        0, 0, cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.BOTTOM,
        new RotateImage(new EmptyImage(), 90), 0, 0,
        cellImg);
    scene.placeImageXY(cellImg, 46, 16);

    // top right img
    cellImg = new RectangleImage(30, 30, OutlineMode.SOLID, new Color(191, 0, 63));
    cellImg = new OverlayOffsetAlign(AlignModeX.LEFT, AlignModeY.MIDDLE,
        new EmptyImage(), 0, 0, cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.RIGHT, AlignModeY.MIDDLE,
        new RectangleImage(3, 30,
            OutlineMode.SOLID,
            Color.BLACK),
        0, 0, cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.TOP,
        new RotateImage(new RectangleImage(3, 30,
            OutlineMode.SOLID,
            Color.BLACK), 90),
        0, 0, cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.BOTTOM,
        new RotateImage(new EmptyImage(), 90), 0, 0,
        cellImg);
    scene.placeImageXY(cellImg, 76, 16);

    // middle left img
    cellImg = new RectangleImage(30, 30, OutlineMode.SOLID, new Color(31, 0, 223));
    cellImg = new OverlayOffsetAlign(AlignModeX.LEFT, AlignModeY.MIDDLE,
        new RectangleImage(3, 30,
            OutlineMode.SOLID,
            Color.BLACK),
        0, 0, cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.RIGHT, AlignModeY.MIDDLE,
        new RectangleImage(3, 30,
            OutlineMode.SOLID,
            Color.BLACK),
        0, 0, cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.TOP,
        new RotateImage(new EmptyImage(), 90), 0, 0,
        cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.BOTTOM,
        new RotateImage(new EmptyImage(), 90), 0, 0,
        cellImg);
    scene.placeImageXY(cellImg, 16, 46);

    // middle middle img
    cellImg = new RectangleImage(30, 30, OutlineMode.SOLID, new Color(127, 0, 127));
    cellImg = new OverlayOffsetAlign(AlignModeX.LEFT, AlignModeY.MIDDLE,
        new RectangleImage(3, 30,
            OutlineMode.SOLID,
            Color.BLACK),
        0, 0, cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.RIGHT, AlignModeY.MIDDLE,
        new RectangleImage(3, 30,
            OutlineMode.SOLID,
            Color.BLACK),
        0, 0, cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.TOP,
        new RotateImage(new EmptyImage(), 90), 0, 0,
        cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.BOTTOM,
        new RotateImage(new EmptyImage(), 90), 0, 0,
        cellImg);
    scene.placeImageXY(cellImg, 46, 46);

    // middle right img
    cellImg = new RectangleImage(30, 30, OutlineMode.SOLID, new Color(223, 0, 31));
    cellImg = new OverlayOffsetAlign(AlignModeX.LEFT, AlignModeY.MIDDLE,
        new RectangleImage(3, 30,
            OutlineMode.SOLID,
            Color.BLACK),
        0, 0, cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.RIGHT, AlignModeY.MIDDLE,
        new RectangleImage(3, 30,
            OutlineMode.SOLID,
            Color.BLACK),
        0, 0, cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.TOP,
        new RotateImage(new EmptyImage(), 90), 0, 0,
        cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.BOTTOM,
        new RotateImage(new RectangleImage(3, 30,
            OutlineMode.SOLID,
            Color.BLACK), 90),
        0, 0, cellImg);
    scene.placeImageXY(cellImg, 76, 46);

    // bottom left img
    cellImg = new RectangleImage(30, 30, OutlineMode.SOLID, new Color(63, 0, 191));
    cellImg = new OverlayOffsetAlign(AlignModeX.LEFT, AlignModeY.MIDDLE,
        new RectangleImage(3, 30,
            OutlineMode.SOLID,
            Color.BLACK),
        0, 0, cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.RIGHT, AlignModeY.MIDDLE,
        new EmptyImage(), 0, 0, cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.TOP,
        new RotateImage(new EmptyImage(), 90), 0, 0,
        cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.BOTTOM,
        new RotateImage(new RectangleImage(3, 30,
            OutlineMode.SOLID,
            Color.BLACK), 90),
        0, 0, cellImg);
    scene.placeImageXY(cellImg, 16, 76);

    // bottom middle img
    cellImg = new RectangleImage(30, 30, OutlineMode.SOLID, new Color(95, 0, 159));
    cellImg = new OverlayOffsetAlign(AlignModeX.LEFT, AlignModeY.MIDDLE,
        new EmptyImage(), 0, 0, cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.RIGHT, AlignModeY.MIDDLE,
        new EmptyImage(), 0, 0, cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.TOP,
        new RotateImage(new EmptyImage(), 90), 0, 0,
        cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.BOTTOM,
        new RotateImage(new RectangleImage(3, 30,
            OutlineMode.SOLID,
            Color.BLACK), 90),
        0, 0, cellImg);
    scene.placeImageXY(cellImg, 46, 76);

    // bottom right img
    cellImg = new RectangleImage(30, 30, OutlineMode.SOLID, new Color(127, 0, 127));
    cellImg = new OverlayOffsetAlign(AlignModeX.LEFT, AlignModeY.MIDDLE,
        new EmptyImage(), 0, 0, cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.RIGHT, AlignModeY.MIDDLE,
        new RectangleImage(3, 30,
            OutlineMode.SOLID,
            Color.BLACK),
        0, 0, cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.TOP,
        new RotateImage(new RectangleImage(3, 30,
            OutlineMode.SOLID,
            Color.BLACK), 90),
        0, 0, cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.BOTTOM,
        new RotateImage(new RectangleImage(3, 30,
            OutlineMode.SOLID,
            Color.BLACK), 90),
        0, 0, cellImg);
    scene.placeImageXY(cellImg, 76, 76);

    WorldScene copyScene = new WorldScene(90, 90);
    DrawGraph g = new DrawGraph(copyScene, 30, topLeft, bottomRight);
    g.drawCells(1);

    t.checkExpect(scene, copyScene);
  }

  // test draw mode 2
  void testDrawMode2(Tester t) {
    init();
    initEdges();
    KruskalsUnionFind kuf = new KruskalsUnionFind(edges);
    while (!kuf.isFinished()) {
      kuf.iterate();
    }

    // testing the image NOW
    // AHAHAHHAHA
    WorldScene scene = new WorldScene(90, 90);

    // top left img
    WorldImage cellImg = new RectangleImage(30, 30, OutlineMode.SOLID,
        new Color(170, 0, 85));
    cellImg = new OverlayOffsetAlign(AlignModeX.LEFT, AlignModeY.MIDDLE,
        new RectangleImage(3, 30,
            OutlineMode.SOLID,
            Color.BLACK),
        0, 0, cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.RIGHT, AlignModeY.MIDDLE,
        new RectangleImage(3, 30,
            OutlineMode.SOLID,
            Color.BLACK),
        0, 0, cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.TOP,
        new RotateImage(new RectangleImage(3, 30,
            OutlineMode.SOLID,
            Color.BLACK), 90),
        0, 0, cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.BOTTOM,
        new RotateImage(new EmptyImage(), 90), 0, 0,
        cellImg);
    scene.placeImageXY(cellImg, 16, 16);

    // top middle img was not visited
    cellImg = new RectangleImage(30, 30, OutlineMode.SOLID, new Color(127, 0, 127));
    cellImg = new OverlayOffsetAlign(AlignModeX.LEFT, AlignModeY.MIDDLE,
        new RectangleImage(3, 30,
            OutlineMode.SOLID,
            Color.BLACK),
        0, 0, cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.RIGHT, AlignModeY.MIDDLE,
        new EmptyImage(), 0, 0, cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.TOP,
        new RotateImage(new RectangleImage(3, 30,
            OutlineMode.SOLID,
            Color.BLACK), 90),
        0, 0, cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.BOTTOM,
        new RotateImage(new EmptyImage(), 90), 0, 0,
        cellImg);
    scene.placeImageXY(cellImg, 46, 16);

    // top right img
    cellImg = new RectangleImage(30, 30, OutlineMode.SOLID, new Color(170, 0, 85));
    cellImg = new OverlayOffsetAlign(AlignModeX.LEFT, AlignModeY.MIDDLE,
        new EmptyImage(), 0, 0, cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.RIGHT, AlignModeY.MIDDLE,
        new RectangleImage(3, 30,
            OutlineMode.SOLID,
            Color.BLACK),
        0, 0, cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.TOP,
        new RotateImage(new RectangleImage(3, 30,
            OutlineMode.SOLID,
            Color.BLACK), 90),
        0, 0, cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.BOTTOM,
        new RotateImage(new EmptyImage(), 90), 0, 0,
        cellImg);
    scene.placeImageXY(cellImg, 76, 16);

    // middle left img
    cellImg = new RectangleImage(30, 30, OutlineMode.SOLID, new Color(127, 0, 127));
    cellImg = new OverlayOffsetAlign(AlignModeX.LEFT, AlignModeY.MIDDLE,
        new RectangleImage(3, 30,
            OutlineMode.SOLID,
            Color.BLACK),
        0, 0, cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.RIGHT, AlignModeY.MIDDLE,
        new RectangleImage(3, 30,
            OutlineMode.SOLID,
            Color.BLACK),
        0, 0, cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.TOP,
        new RotateImage(new EmptyImage(), 90), 0, 0,
        cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.BOTTOM,
        new RotateImage(new EmptyImage(), 90), 0, 0,
        cellImg);
    scene.placeImageXY(cellImg, 16, 46);

    // middle middle img
    cellImg = new RectangleImage(30, 30, OutlineMode.SOLID, new Color(85, 0, 170));
    cellImg = new OverlayOffsetAlign(AlignModeX.LEFT, AlignModeY.MIDDLE,
        new RectangleImage(3, 30,
            OutlineMode.SOLID,
            Color.BLACK),
        0, 0, cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.RIGHT, AlignModeY.MIDDLE,
        new RectangleImage(3, 30,
            OutlineMode.SOLID,
            Color.BLACK),
        0, 0, cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.TOP,
        new RotateImage(new EmptyImage(), 90), 0, 0,
        cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.BOTTOM,
        new RotateImage(new EmptyImage(), 90), 0, 0,
        cellImg);
    scene.placeImageXY(cellImg, 46, 46);

    // middle right img
    cellImg = new RectangleImage(30, 30, OutlineMode.SOLID, new Color(212, 0, 42));
    cellImg = new OverlayOffsetAlign(AlignModeX.LEFT, AlignModeY.MIDDLE,
        new RectangleImage(3, 30,
            OutlineMode.SOLID,
            Color.BLACK),
        0, 0, cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.RIGHT, AlignModeY.MIDDLE,
        new RectangleImage(3, 30,
            OutlineMode.SOLID,
            Color.BLACK),
        0, 0, cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.TOP,
        new RotateImage(new EmptyImage(), 90), 0, 0,
        cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.BOTTOM,
        new RotateImage(new RectangleImage(3, 30,
            OutlineMode.SOLID,
            Color.BLACK), 90),
        0, 0, cellImg);
    scene.placeImageXY(cellImg, 76, 46);

    // bottom left img
    cellImg = new RectangleImage(30, 30, OutlineMode.SOLID, new Color(85, 0, 170));
    cellImg = new OverlayOffsetAlign(AlignModeX.LEFT, AlignModeY.MIDDLE,
        new RectangleImage(3, 30,
            OutlineMode.SOLID,
            Color.BLACK),
        0, 0, cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.RIGHT, AlignModeY.MIDDLE,
        new EmptyImage(), 0, 0, cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.TOP,
        new RotateImage(new EmptyImage(), 90), 0, 0,
        cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.BOTTOM,
        new RotateImage(new RectangleImage(3, 30,
            OutlineMode.SOLID,
            Color.BLACK), 90),
        0, 0, cellImg);
    scene.placeImageXY(cellImg, 16, 76);

    // bottom middle img
    cellImg = new RectangleImage(30, 30, OutlineMode.SOLID, new Color(42, 0, 212));
    cellImg = new OverlayOffsetAlign(AlignModeX.LEFT, AlignModeY.MIDDLE,
        new EmptyImage(), 0, 0, cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.RIGHT, AlignModeY.MIDDLE,
        new EmptyImage(), 0, 0, cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.TOP,
        new RotateImage(new EmptyImage(), 90), 0, 0,
        cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.BOTTOM,
        new RotateImage(new RectangleImage(3, 30,
            OutlineMode.SOLID,
            Color.BLACK), 90),
        0, 0, cellImg);
    scene.placeImageXY(cellImg, 46, 76);

    // bottom right img
    cellImg = new RectangleImage(30, 30, OutlineMode.SOLID, new Color(0, 0, 255));
    cellImg = new OverlayOffsetAlign(AlignModeX.LEFT, AlignModeY.MIDDLE,
        new EmptyImage(), 0, 0, cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.RIGHT, AlignModeY.MIDDLE,
        new RectangleImage(3, 30,
            OutlineMode.SOLID,
            Color.BLACK),
        0, 0, cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.TOP,
        new RotateImage(new RectangleImage(3, 30,
            OutlineMode.SOLID,
            Color.BLACK), 90),
        0, 0, cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.BOTTOM,
        new RotateImage(new RectangleImage(3, 30,
            OutlineMode.SOLID,
            Color.BLACK), 90),
        0, 0, cellImg);
    scene.placeImageXY(cellImg, 76, 76);

    WorldScene copyScene = new WorldScene(90, 90);
    DrawGraph g = new DrawGraph(copyScene, 30, topLeft, bottomRight);
    g.drawCells(2);
    // YAY!!!
    t.checkExpect(scene, copyScene);
  }

  // testing moveFrom
  void testMoveFrom(Tester t) {
    init();
    initEdges();
    KruskalsUnionFind kuf = new KruskalsUnionFind(edges);
    while (!kuf.isFinished()) {
      kuf.iterate();
    }
    t.checkExpect(middleLeft.movedFrom(topLeft), middleLeft);
    t.checkExpect(topLeft.movedFrom(middleLeft), topLeft);
    t.checkExpect(new MtCell().movedFrom(topLeft), topLeft);
  }

  void testBFSInGraph(Tester t) {
    init();
    initEdges();
    KruskalsUnionFind kuf = new KruskalsUnionFind(edges);
    while (!kuf.isFinished()) {
      kuf.iterate();
    }

    SearchHelper bfs = new SearchHelper(topLeft, bottomRight, new Queue<>());
    while (!bfs.isFinished()) {
      bfs.iterate();
    }
    // skips the wallbreaking animation
    graph.skipAnimation();
    graph.bfs();
    // finishes bfs
    graph.skipAnimation();

    // should have the same completed bfs!
    WorldScene scene = new WorldScene(750, 750);
    new DrawGraph(scene, 250, topLeft, bottomRight).drawCells(0);
    t.checkExpect(graph.draw(0), scene);
  }

  // testing the dfs animation
  void testDFSInGraph(Tester t) {
    init();
    initEdges();
    KruskalsUnionFind kuf = new KruskalsUnionFind(edges);
    while (!kuf.isFinished()) {
      kuf.iterate();
    }

    SearchHelper dfs = new SearchHelper(topLeft, bottomRight, new Stack<>());
    while (!dfs.isFinished()) {
      dfs.iterate();
    }
    // skips the wallbreaking animation
    graph.skipAnimation();
    graph.dfs();
    // finishes dfs
    graph.skipAnimation();

    // should have the same completed bfs!
    WorldScene scene = new WorldScene(750, 750);
    new DrawGraph(scene, 250, topLeft, bottomRight).drawCells(0);
    t.checkExpect(graph.draw(0), scene);
  }

  void testSkipAnimation(Tester t) {
    init();
    initEdges();
    // kuf that gets ran thru
    KruskalsUnionFind kuf = new KruskalsUnionFind(edges);
    kuf.iterate();
    WorldScene scene = new WorldScene(750, 750);
    new DrawGraph(scene, 250, topLeft, bottomRight).drawCells(0);
    t.checkExpect(graph.draw(0), scene);

    // for each step of the animation, the scenes are the same
    while (!kuf.isFinished()) {
      kuf.iterate();
      scene = new WorldScene(750, 750);
      new DrawGraph(scene, 250, topLeft, bottomRight).drawCells(0);
      t.checkExpect(graph.draw(0), scene);
    }

    // now doing skip
    init();
    initEdges();
    kuf = new KruskalsUnionFind(edges);
    while (!kuf.isFinished()) {
      kuf.iterate();
    }
    graph.skipAnimation();
    scene = new WorldScene(750, 750);
    new DrawGraph(scene, 250, topLeft, bottomRight).drawCells(0);
    t.checkExpect(graph.draw(0), scene);

    // testing every step of bfs
    SearchHelper bfs = new SearchHelper(topLeft, bottomRight, new Queue<>());
    graph.bfs();
    while (!bfs.isFinished()) {
      bfs.iterate();
      scene = new WorldScene(750, 750);
      new DrawGraph(scene, 250, topLeft, bottomRight).drawCells(0);
      t.checkExpect(graph.draw(0), scene);
    }
  }

  // drawing with diff modes lol
  void testDiffModesDrawGraph(Tester t) {
    init();
    initEdges();
    KruskalsUnionFind kuf = new KruskalsUnionFind(edges);
    while (!kuf.isFinished()) {
      kuf.iterate();
    }
    graph.skipAnimation();

    WorldScene scene = new WorldScene(750, 750);
    new DrawGraph(scene, 250, topLeft, bottomRight).drawCells(0);
    t.checkExpect(graph.draw(0), scene);

    scene = new WorldScene(750, 750);
    new DrawGraph(scene, 250, topLeft, bottomRight).drawCells(1);
    t.checkExpect(graph.draw(1), scene);

    scene = new WorldScene(750, 750);
    new DrawGraph(scene, 250, topLeft, bottomRight).drawCells(2);
    t.checkExpect(graph.draw(2), scene);

    SearchHelper dfs = new SearchHelper(topLeft, bottomRight, new Stack<>());
    while (!dfs.isFinished()) {
      dfs.iterate();
    }
    graph.dfs();
    graph.skipAnimation();

    scene = new WorldScene(750, 750);
    new DrawGraph(scene, 250, topLeft, bottomRight).drawCells(0);
    t.checkExpect(graph.draw(0), scene);

    scene = new WorldScene(750, 750);
    new DrawGraph(scene, 250, topLeft, bottomRight).drawCells(1);
    t.checkExpect(graph.draw(1), scene);

    scene = new WorldScene(750, 750);
    new DrawGraph(scene, 250, topLeft, bottomRight).drawCells(2);
    t.checkExpect(graph.draw(2), scene);

    init();
    initEdges();
    kuf = new KruskalsUnionFind(edges);
    while (!kuf.isFinished()) {
      kuf.iterate();
    }
    graph.skipAnimation();

    SearchHelper bfs = new SearchHelper(topLeft, bottomRight, new Queue<>());
    while (!bfs.isFinished()) {
      bfs.iterate();
    }
    graph.bfs();
    graph.skipAnimation();

    scene = new WorldScene(750, 750);
    new DrawGraph(scene, 250, topLeft, bottomRight).drawCells(0);
    t.checkExpect(graph.draw(0), scene);

    scene = new WorldScene(750, 750);
    new DrawGraph(scene, 250, topLeft, bottomRight).drawCells(1);
    t.checkExpect(graph.draw(1), scene);

    scene = new WorldScene(750, 750);
    new DrawGraph(scene, 250, topLeft, bottomRight).drawCells(2);
    t.checkExpect(graph.draw(2), scene);

  }

  // testing onKeys
  void testOnKey(Tester t) {
    init();
    initEdges();
    KruskalsUnionFind kuf = new KruskalsUnionFind(edges);
    while (!kuf.isFinished()) {
      kuf.iterate();
    }

    // skips the wallBreaking animation
    game.onKeyEvent("s");

    WorldScene scene = new WorldScene(750, 750);
    new DrawGraph(scene, 250, topLeft, bottomRight).drawCells(0);
    t.checkExpect(game.makeScene(), scene);

    // changes the color mode to 1
    game.onKeyEvent("c");
    scene = new WorldScene(750, 750);
    new DrawGraph(scene, 250, topLeft, bottomRight).drawCells(1);
    t.checkExpect(game.makeScene(), scene);

    // changes the color mode to 2
    game.onKeyEvent("c");
    scene = new WorldScene(750, 750);
    new DrawGraph(scene, 250, topLeft, bottomRight).drawCells(2);
    t.checkExpect(game.makeScene(), scene);

    // color mode 0
    game.onKeyEvent("c");
    scene = new WorldScene(750, 750);
    new DrawGraph(scene, 250, topLeft, bottomRight).drawCells(0);
    t.checkExpect(game.makeScene(), scene);

    // starting the dfs animation then resetting it
    game.onKeyEvent("d");
    game.onKeyEvent("s");
    SearchHelper dfs = new SearchHelper(topLeft, bottomRight, new Stack<>());
    while (!dfs.isFinished()) {
      dfs.iterate();
    }
    new DrawGraph(scene, 250, topLeft, bottomRight).drawCells(0);
    t.checkExpect(game.makeScene(), scene);

    init();
    initEdges();
    kuf = new KruskalsUnionFind(edges);
    while (!kuf.isFinished()) {
      kuf.iterate();
    }
    game.onKeyEvent("s");

    // trying a bfs
    game.onKeyEvent("b");
    game.onKeyEvent("s");
    SearchHelper bfs = new SearchHelper(topLeft, bottomRight, new Queue<>());
    while (!bfs.isFinished()) {
      bfs.iterate();
    }
    new DrawGraph(scene, 250, topLeft, bottomRight).drawCells(0);
    t.checkExpect(game.makeScene(), scene);

    init();
    initEdges();
    kuf = new KruskalsUnionFind(edges);
    while (!kuf.isFinished()) {
      kuf.iterate();
    }
    game.onKeyEvent("s");
    game.onKeyEvent("m");
    // using manual mode keys
    this.topLeft.highlight();
    scene = new WorldScene(750, 750);
    new DrawGraph(scene, 250, topLeft, bottomRight).drawCells(0);
    t.checkExpect(game.makeScene(), scene);

    game.onKeyEvent("down");
    topLeft.moveDown();
    scene = new WorldScene(750, 750);
    new DrawGraph(scene, 250, topLeft, bottomRight).drawCells(0);
    t.checkExpect(game.makeScene(), scene);

    game.onKeyEvent("right");
    middleLeft.moveRight();
    scene = new WorldScene(750, 750);
    new DrawGraph(scene, 250, topLeft, bottomRight).drawCells(0);
    t.checkExpect(game.makeScene(), scene);

    game.onKeyEvent("left");
    middleLeft.moveLeft();
    scene = new WorldScene(750, 750);
    new DrawGraph(scene, 250, topLeft, bottomRight).drawCells(0);
    t.checkExpect(game.makeScene(), scene);

    game.onKeyEvent("up");
    middleLeft.moveUp();
    scene = new WorldScene(750, 750);
    new DrawGraph(scene, 250, topLeft, bottomRight).drawCells(0);
    t.checkExpect(game.makeScene(), scene);

    game.onKeyEvent("down");
    topLeft.moveDown();
    game.onKeyEvent("down");
    middleLeft.moveDown();
    scene = new WorldScene(750, 750);
    new DrawGraph(scene, 250, topLeft, bottomRight).drawCells(0);
    t.checkExpect(game.makeScene(), scene);

    game.onKeyEvent("adfsafdf");
    scene = new WorldScene(750, 750);
    new DrawGraph(scene, 250, topLeft, bottomRight).drawCells(0);
    t.checkExpect(game.makeScene(), scene);

  }

  // Tests for the Stack class
  public void testStack(Tester t) {
    // Test isEmpty() method
    Stack<Integer> stack1 = new Stack<Integer>();
    t.checkExpect(stack1.isEmpty(), true);

    stack1.add(1);
    t.checkExpect(stack1.isEmpty(), false);

    stack1.remove();
    t.checkExpect(stack1.isEmpty(), true);

    // Test add() and remove() methods
    Stack<String> stack2 = new Stack<String>();
    stack2.add("a");
    stack2.add("b");
    stack2.add("c");
    t.checkExpect(stack2.remove(), "c");
    t.checkExpect(stack2.remove(), "b");
    t.checkExpect(stack2.remove(), "a");

    // Test removing from an empty stack
    Stack<Boolean> stack3 = new Stack<Boolean>();
    t.checkException(new NoSuchElementException(), stack3, "remove");
  }

  // Tests for the Queue class
  public void testQueue(Tester t) {
    // Test isEmpty() method
    Queue<Integer> queue1 = new Queue<Integer>();
    t.checkExpect(queue1.isEmpty(), true);

    queue1.add(1);
    t.checkExpect(queue1.isEmpty(), false);

    queue1.remove();
    t.checkExpect(queue1.isEmpty(), true);

    // Test add() and remove() methods
    Queue<String> queue2 = new Queue<String>();
    queue2.add("a");
    queue2.add("b");
    queue2.add("c");
    t.checkExpect(queue2.remove(), "a");
    t.checkExpect(queue2.remove(), "b");
    t.checkExpect(queue2.remove(), "c");

    // Test removing from an empty queue
    Queue<Boolean> queue3 = new Queue<Boolean>();
    t.checkException(new NoSuchElementException(), queue3, "remove");
  }

}

import java.awt.Color;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.Random;
import javalib.impworld.*;
import javalib.worldimages.AlignModeX;
import javalib.worldimages.AlignModeY;
import javalib.worldimages.OutlineMode;
import javalib.worldimages.OverlayOffsetAlign;
import javalib.worldimages.Posn;
import javalib.worldimages.RectangleImage;
import javalib.worldimages.RotateImage;
import javalib.worldimages.WorldImage;

// This helper class is intended to help render out a square maze
class DrawGraph {
  private final WorldScene scene;
  // the size of each cell
  private final int cellDimensions;
  // the beginning cell of the maze
  private final Cell topLeft;
  // the ending cell of the maze
  private final Cell bottomRight;
  // furthest distance from origin
  // in order to help drawing the heatmap
  private int furthestFromOrigin;

  DrawGraph(WorldScene scene, int cellDimensions, Cell topLeft, Cell bottomRight) {
    this.scene = scene;
    this.cellDimensions = cellDimensions;
    this.topLeft = topLeft;
    this.furthestFromOrigin = 0;
    this.bottomRight = bottomRight;
  }

  // draws every cell in the maze, starting with topLeft
  void drawCells(int mode) {
    // if the mode is 1, calculates the furthestFromOrigin int relative to topLeft
    if (mode == 1) {
      this.furthestFromOrigin = topLeft.calculateDistanceFromThisCell();
    }
    // if the mode is 2, calculates furtheastFromOrigin relative to the bottomRight
    else if (mode == 2) {
      this.furthestFromOrigin = bottomRight.calculateDistanceFromThisCell();
      // makes mode 1 because this has the same effect inside the cell class
      mode = 1;
    }
    topLeft.draw(this, true, mode, this.furthestFromOrigin);
  }

  // Confusing method!!!
  // This is called upon by the Cell class, treating the DrawGraph as a visitor.
  // The cell passes in its posn, edges, and
  // color based on the given mode
  void drawCellRow(ICell cameFrom, Posn placeImgPosn,
      Edge left, Edge right, Edge up, Edge down, Color color,
      boolean firstCall, int mode) {

    // since the posn of a cell is based on indices and not cell dimensions, changes
    // the posn to be on the right scale
    placeImgPosn = new Posn((placeImgPosn.x * this.cellDimensions) +
        (cellDimensions / 2) + 1,
        (placeImgPosn.y * this.cellDimensions)
            + (this.cellDimensions / 2) + 1);

    // creates the basic square cell
    WorldImage cellImg = new RectangleImage(this.cellDimensions,
        this.cellDimensions, OutlineMode.SOLID, color);
    // draws the walls surrounding the cell
    cellImg = new OverlayOffsetAlign(AlignModeX.LEFT, AlignModeY.MIDDLE,
        left.drawWall(cellDimensions),
        0, 0, cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.RIGHT, AlignModeY.MIDDLE,
        right.drawWall(cellDimensions),
        0, 0, cellImg);

    cellImg = new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.TOP,
        new RotateImage(up.drawWall(cellDimensions), 90),
        0, 0, cellImg);
    cellImg = new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.BOTTOM,
        new RotateImage(down.drawWall(cellDimensions), 90),
        0, 0, cellImg);
    scene.placeImageXY(cellImg, placeImgPosn.x, placeImgPosn.y);

    // draws all of the cells to the right of this cell, but firstCall is false,
    // because we don't
    // want these cells to draw the cells below
    right.drawRightCells(cameFrom, this, false, mode,
        this.furthestFromOrigin);

    // if firstcall is true, we want to draw the row below this cell as well
    if (firstCall) {
      down.drawRightCells(cameFrom, this, true, mode,
          this.furthestFromOrigin);
    }

  }
}

// Help interface used to iterate through different animations in the maze game
interface IAnimationHelper {
  // on call, to iterate one step through the animation
  void iterate();

  // to determine if a animation is done
  boolean isFinished();
}

// This class is used for maze generation, unionizing one edge
// on each iteration (Extra credit)
class KruskalsUnionFind implements IAnimationHelper {
  private final ArrayList<Edge> edges;
  private final HashMap<Posn, Posn> representatives;
  private int index;

  KruskalsUnionFind(ArrayList<Edge> edges) {
    this.edges = edges;
    this.edges.sort(new CompareEdges());
    this.representatives = new HashMap<>();
    this.index = 0;
    this.putEdges();
  }

  // To find the representative of a given position (cell) in the HashMap
  Posn find(Posn key) {
    if (this.representatives.get(key).equals(key)) {
      return key;
    } else {
      Posn root = this.find(this.representatives.get(key));
      representatives.put(key, root);
      return root;
    }
  }

  // To unionize two specific positions (cells) in the HashMap,
  // possibly combining two trees.
  // EFFECT: Mutates the represntative tree, replacing root1's
  // value with root2
  void union(Posn root1, Posn root2) {
    this.representatives.put(root1, root2);
  }

  // To create the minimum spanning tree of our randomized maze graph
  // EFFECT: adds all edges to a given HashMap representing a spanning tree
  void putEdges() {

    for (Edge e : edges) {
      e.addNodes(this.representatives);
    }

  }

  // To determine if the maze is finished generating
  public boolean isFinished() {
    return this.edges.size() <= this.index;
  }

  // To move forward through states of maze generation, unionizing one step at a
  // time
  // EFFECT: Mutates the edges in each list,
  // unionzing them or not changing then depending on equality
  public void iterate() {
    if (!this.isFinished()) {
      this.edges.get(index).kruskalsHelper(this);
      this.index += 1;
    }
  }

  // Unionizes cells and mutates the edge wall field to
  // indicate connecting two cells representationally
  // EFFECT: To connect cells using union if cells are not already connected,
  // which mutates the HashMap spanning
  void connectCells(Edge from, ICell cell1, ICell cell2) {
    Posn root1 = this.find(cell1.getId());
    Posn root2 = this.find(cell2.getId());

    // if both cells don't have the same root
    if (!root1.equals(root2)) {
      from.breakWall();
      this.union(root1, root2);
    }
  }
}

// This class helps iterate through each step of both BFS and DFS search
// algorithims
class SearchHelper implements IAnimationHelper {
  private final Deque<Cell> alreadySeen;
  private final Cell to;
  private final ICollection<Cell> worklist;
  private boolean finished;

  SearchHelper(Cell from, Cell to, ICollection<Cell> worklist) {
    this.alreadySeen = new ArrayDeque<Cell>();
    this.to = to;
    this.worklist = worklist;
    // putting the first cell in the owrklist
    from.addToList(worklist);
    this.finished = false;
  }

  // To help move through each step of the search algorithim, used for animation
  // EFFECT: mutates the worklist, removing the next value and adds to already
  // seen
  // in order to keep track of location in the search algorithim.
  public void iterate() {
    if (!this.isFinished()) {
      Cell next = worklist.remove();
      // if next is the target, highlight it and the rest of the solution path
      if (next == this.to) {
        next.highlight();
        this.finished = true;
      }
      // otherwise, add the neighbors of next to the worklist
      else if (!alreadySeen.contains(next)) {
        next.addNeighborsToWorklist(this.worklist);
      }

      this.alreadySeen.add(next);
    }
  }

  // To help determine if the maze has reached the target cell,
  // which indicates that the search algorithm has found the end cell.
  public boolean isFinished() {
    return this.worklist.isEmpty() || this.finished;
  }

}

// Represents a comparator to sort a list of edges according to weight
class CompareEdges implements Comparator<Edge> {
  // Compares two edges, and returns a negative, 0, or positive number
  // in order help the sort algorithm for the edge ArrayList
  public int compare(Edge edge1, Edge edge2) {
    return edge1.compare(edge2);
  }
}

// Represents a Utils class that contains useful tools.
class Utils {
  // to create edges with a given set of cells, uses a given seed for the
  // randomization,
  // and generates according to input biases where the higher bias ratio favoring
  // vertical
  // or horizontal pocket generation.
  ArrayList<Edge> createEdges(ArrayList<ArrayList<Cell>> cells,
      int seed, double vertBiasFactor,
      double horizBiasFactor) {
    ArrayList<Edge> edges = new ArrayList<>();
    Random r = new Random(seed);

    // for each row in the cells arraylist
    for (int rowIndex = 0; rowIndex < cells.size(); rowIndex += 1) {

      // for each column in that row
      for (int colIndex = 0; colIndex < cells.get(rowIndex).size(); colIndex += 1) {

        Cell currCell = cells.get(rowIndex).get(colIndex);

        // if the cell is on the top
        // create an up edge with an empty cell
        if (rowIndex == 0) {
          Edge edge = new Edge(currCell, new MtCell(), 0);
          currCell.updateUpEdge(edge);
        }
        // otherwise, create a new up edge with this cell and the cell above it
        else {
          Edge edge = new Edge(currCell, cells.get(rowIndex - 1).get(colIndex),
              (int) (r.nextInt(1000) * horizBiasFactor));
          edges.add(edge);
          currCell.updateUpEdge(edge);
          cells.get(rowIndex - 1).get(colIndex).updateDownEdge(edge);
        }

        // if this cell is on far right, create a right edge with an empty cell
        if (colIndex == cells.get(rowIndex).size() - 1) {
          Edge edge = new Edge(currCell, new MtCell(), 0);
          currCell.updateRightEdge(edge);
        }
        // else create an edge with this cell and the right cell
        else {
          Edge edge = new Edge(currCell, cells.get(rowIndex).get(colIndex + 1),
              (int) (r.nextInt(1000) * vertBiasFactor));
          edges.add(edge);
          currCell.updateRightEdge(edge);
          cells.get(rowIndex).get(colIndex + 1).updateLeftEdge(edge);
        }

        // if this is a far left cell, make a left edge with an empty cell
        if (colIndex == 0) {
          Edge edge = new Edge(currCell, new MtCell(),
              0);
          currCell.updateLeftEdge(edge);
        }

        // if this is a bottom cell, create a down edge with an empty cell
        if (rowIndex == cells.size() - 1) {
          Edge edge = new Edge(currCell, new MtCell(),
              0);
          currCell.updateDownEdge(edge);
        }
      }
    }

    return edges;
  }
}
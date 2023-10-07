import java.util.ArrayDeque;
import java.util.Deque;

// Represents a mutable collection of items
interface ICollection<T> {
  // Is this collection empty?
  boolean isEmpty();

  // EFFECT: adds the item to the collection
  void add(T item);

  // Returns the first item of the collection
  // EFFECT: removes that first item
  T remove();
}

// represents a stack, where you both add and remove elements at the front
class Stack<T> implements ICollection<T> {
  private final Deque<T> contents;

  Stack() {
    this.contents = new ArrayDeque<T>();
  }

  // returns true if there are no elements in the stack
  public boolean isEmpty() {
    return this.contents.isEmpty();
  }

  // returns the first element in the stack
  public T remove() {
    return this.contents.removeFirst();
  }

  // adds an element to the beginning of the stack
  public void add(T item) {
    this.contents.addFirst(item);
  }
}

// represents a queue, where you can add elements at the
// back and remove them from the front
class Queue<T> implements ICollection<T> {
  private final Deque<T> contents;

  Queue() {
    this.contents = new ArrayDeque<T>();
  }

  // returns true if it's empty
  public boolean isEmpty() {
    return this.contents.isEmpty();
  }

  // returns the first element of the queue(the front)
  public T remove() {
    return this.contents.removeFirst();
  }

  // adds an element to the back of the queue
  public void add(T item) {
    this.contents.addLast(item);
  }
}

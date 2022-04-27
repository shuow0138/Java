package cmsc420_s22;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;

public class QuakeHeap<Key extends Comparable<Key>, Value> {

	// -----------------------------------------------------------------
	// Node
	// -----------------------------------------------------------------

	class Node {
		Key key; // key (for sorting)
		final Value value; // value (application dependent)
		int level; // level in the tree (leaf = level 0)
		Node left; // children
		Node right;
		Node parent; // parent

		/**
		 * Basic constructor.
		 */
		Node(Key x, Value v, int level, Node left, Node right) {
			this.key = x;
			this.value = v;
			this.level = level;
			this.left = left;
			this.right = right;
			this.parent = null;
		}

		/**
		 * Minimal constructor (for leaves).
		 */
		Node(Key x, Value v) {
			this.key = x;
			this.value = v;
			this.level = 0;
			this.left = this.right = this.parent = null;
		}
	}

	/**
	 * Node comparator used for sorting root nodes.
	 */

	private class ByKey implements Comparator<Node> {
		public int compare(Node u, Node v) {
			if (u.key.compareTo(v.key) < 0)
				return -1;
			else if (u.key.compareTo(v.key) == 0)
				return 0;
			else
				return +1;
		}
	}

	// -----------------------------------------------------------------
	// Locator - Used to locate a previously inserted item
	// The Node reference always points to a leaf node.
	// -----------------------------------------------------------------

	public class Locator {
		private final Node u;

		private Locator(Node u) { // basic constructor
			this.u = u;
		}

		private Node get() { // get the associated node
			return u;
		}
	}

	// -----------------------------------------------------------------
	// Private members
	// -----------------------------------------------------------------

	private int nLevels; // number of levels
	private LinkedList<Node>[] roots; // list of roots per level
	private int[] nodeCt; // number of nodes per level

	// -----------------------------------------------------------------
	// Local utilities
	// -----------------------------------------------------------------

	/**
	 * Add a new node as root.
	 */
	void makeRoot(Node u) {
		u.parent = null; // null out parent link
		roots[u.level].add(u); // add node at u's level
	}

	/**
	 * Create a trivial single-node tree
	 */
	Node trivialTree(Key x, Value v) { // create a trivial single-node tree
		Node u = new Node(x, v); // create new leaf node
		nodeCt[0] += 1; // increment node count
		makeRoot(u); // make it a root
		return u;
	}

	/**
	 * Link trees rooted at u and v (from same level) together to form a new tree
	 * (one level higher). By convention, the smaller key is stored in the left
	 * subtree. The tasks of removing u and v from the list of roots and adding the
	 * new root w is handled in the calling function.
	 */
	Node link(Node u, Node v) { // link u and v into new tree
		assert (u.level == v.level); // nodes must be at the same level
		Node w; // the new node
		int lev = u.level + 1; // w's level
		if (u.key.compareTo(v.key) <= 0) { // u's key is smaller?
			w = new Node(u.key, null, lev, u, v); // new root with u's key
		} else {
			w = new Node(v.key, null, lev, v, u); // new root with v's key
		}
		nodeCt[lev] += 1; // increment node count
		u.parent = v.parent = w; // w is the new parent
		return w;
	}

	/**
	 * Search the roots of all the trees and return a reference to the one having
	 * the smallest key value.
	 */
	Node findRootWithSmallestKey() {
		Node min = null;
		for (int lev = 0; lev < nLevels; lev++) { // process all levels
			for (Node u : roots[lev]) {
				if (min == null) {
					min = u;
				} else if (u.key.compareTo(min.key) < 0) {
					min = u;
				}
			}
		}
		return min;
	}

	/**
	 * Merge all pairs of trees at the same level. We work bottom-up because merging
	 * two trees creates a tree one level higher, which can then be merged with
	 * others. Note that we stop merging at nLevels-2, since we cannot create nodes
	 * above that level.
	 * 
	 * Alert: We sort the roots by key value. This is not part of the QuakeHeap
	 * algorithm. It is done for the sake of having deterministic behavior.
	 */
	void mergeTrees() {
		for (int lev = 0; lev < nLevels - 1; lev++) { // process levels bottom-up
			Collections.sort(roots[lev], new ByKey()); // sort roots by key
			while (roots[lev].size() >= 2) { // at least two trees?
				Node u = roots[lev].remove(); // remove two trees
				Node v = roots[lev].remove();
				Node w = link(u, v); // ... and merge them
				makeRoot(w); // ... and make this a root
			}
		}
	}

	/**
	 * Get a list of the nodes in preorder of a single subtree.
	 */
	ArrayList<String> getPreorderList(Node u) {
		ArrayList<String> list = new ArrayList<String>();
		if (u == null) {
			list.add("[null]");
		} else if (u.level > 0) {
			list.add("(" + u.key + ")");
			list.addAll(getPreorderList(u.left));
			list.addAll(getPreorderList(u.right));
		} else {
			list.add("[" + u.key + " " + u.value + "]");
		}
		return list;
	}

	// -----------------------------------------------------------------
	// Public members
	// -----------------------------------------------------------------

	/**
	 * Default constructor.
	 */
	@SuppressWarnings("unchecked")
	public QuakeHeap(int nLevels) {
		this.nLevels = nLevels;
		roots = new LinkedList[nLevels];
		nodeCt = new int[nLevels];
		for (int i = 0; i < nLevels; i++) {
			roots[i] = new LinkedList<Node>();
			nodeCt[i] = 0;
		}
	}

	/**
	 * Clear the entire structure.
	 */
	public void clear() {
		for (int lev = 0; lev < nLevels; lev++) {
			roots[lev].clear();
			nodeCt[lev] = 0;
		}
	}

	/**
	 * Insert key-value pair.
	 */
	public Locator insert(Key x, Value v) {
		Node u = trivialTree(x, v); // create a one-node tree storing x
		return new Locator(u); // return a reference to it
	}

	/**
	 * Get the minimum key from the heap. In addition to returning the minimum, this
	 * also applies the merging and quaking part of the reorganization.
	 */
	public Key getMinKey() throws Exception {
		Node u = findRootWithSmallestKey(); // find the min root
		if (u == null) { // heap is empty
			throw new Exception("Empty heap");
		}
		mergeTrees(); // merge trees
		return u.key;
	}

	/**
	 * Get the maximum level of for the entry specified by locator r. This is
	 * defined to be the length of the longest chain of left-child links leading to
	 * the leaf node referenced by r.
	 */
	public int getMaxLevel(Locator r) {
		Node u = r.get(); // leaf node to be changed
		while (u.parent != null && u == u.parent.left) { // climb up left links
			u = u.parent;
		}
		return u.level;
	}

	/**
	 * Get a list of entries in preorder.
	 */
	public ArrayList<String> listHeap() {
		ArrayList<String> list = new ArrayList<String>();
		for (int lev = 0; lev < nLevels; lev++) {
			if (nodeCt[lev] > 0) {
				list.add("{lev: " + lev + " nodeCt: " + nodeCt[lev] + "}");
			}
			if (roots[lev].size() > 0) { // has at least one root?
				Collections.sort(roots[lev], new ByKey()); // sort roots by key
				for (Node u : roots[lev]) {
					list.addAll(getPreorderList(u));
				}
			}
		}
		return list;
	}

}

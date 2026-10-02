// use of ChatGPT (GPT-5.6 Luna) for creating initial code. Later modified and tested thouroughly to match the requirements
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

/**
 * B+ Tree assignment implementation.
 *
 * Updated assignment requirements:
 *   - Entirely in memory.
 *   - Program starts with: java bplus init <d>
 *   - Leaf capacity: at most 2d records, at least d records except root.
 *   - Internal capacity: at most 2d keys / 2d+1 children,
 *     at least d keys except root.
 *   - Integer keys and integer pointers.
 *   - No duplicate keys.
 *   - No persistence or simulated disk I/O.
 *
 * Input commands:
 *   INSERT key pointer
 *   DELETE key
 *   SEARCH key
 *   RANGESEARCH k1 k2
 *   PRINT
 *   PRINT STATISTICS
 */
public class bplus {

    /* ------------------------- Node classes ------------------------- */

    private abstract static class Node {
        InternalNode parent;

        abstract boolean isLeaf();

        abstract int firstKey();
    }

    /**
     * Leaf nodes contain the actual key-pointer pairs.
     * Leaves are connected through next/previous pointers to support
     * efficient range searches.
     */
    private static class LeafNode extends Node {
        List<Integer> keys = new ArrayList<>();
        List<Integer> pointers = new ArrayList<>();

        LeafNode next;
        LeafNode previous;

        @Override
        boolean isLeaf() {
            return true;
        }

        @Override
        int firstKey() {
            return keys.get(0);
        }
    }

    /**
     * Internal nodes contain separator keys and child pointers.
     *
     * For an internal node:
     *   number of children = number of keys + 1
     *
     * A separator key at index i is the first key in children[i + 1].
     */
    private static class InternalNode extends Node {
        List<Integer> keys = new ArrayList<>();
        List<Node> children = new ArrayList<>();

        @Override
        boolean isLeaf() {
            return false;
        }

        @Override
        int firstKey() {
            return children.get(0).firstKey();
        }
    }

    /* ------------------------- Result class ------------------------- */

    private static class SearchResult {
        LeafNode leaf;
        int index;

        SearchResult(LeafNode leaf, int index) {
            this.leaf = leaf;
            this.index = index;
        }
    }

    /* ------------------------- B+ Tree ------------------------- */

    private static class BPlusTree {
        private final int d;
        private final int maxLeafEntries;
        private final int maxInternalKeys;

        private Node root;

        BPlusTree(int d) {
            if (d < 1) {
                throw new IllegalArgumentException("d must be at least 1.");
            }

            this.d = d;
            this.maxLeafEntries = 2 * d;
            this.maxInternalKeys = 2 * d;

            // Start with an empty leaf root.
            this.root = new LeafNode();
        }

        /* ============================================================
         * SEARCH
         * ============================================================ */

        /**
         * Returns the pointer associated with key, or -1 if not found.
         */
        int search(int key) {
            LeafNode leaf = findLeaf(key);
            int index = Collections.binarySearch(leaf.keys, key);

            if (index >= 0) {
                return leaf.pointers.get(index);
            }

            return -1;
        }

        /**
         * Prints the required search message.
         */
        void searchAndPrint(int key) {
            int pointer = search(key);

            if (pointer != -1) {
                System.out.println(key + " found, point is " + pointer);
            } else {
                System.out.println(key + " not found");
            }
        }

        /* ============================================================
         * INSERT
         * ============================================================ */

        /**
         * Inserts key-pointer pair if key does not already exist.
         */
        boolean insert(int key, int pointer) {
            LeafNode leaf = findLeaf(key);
            int position = Collections.binarySearch(leaf.keys, key);

            // Duplicate key.
            if (position >= 0) {
                return false;
            }

            int insertAt = -position - 1;

            leaf.keys.add(insertAt, key);
            leaf.pointers.add(insertAt, pointer);

            if (leaf.keys.size() > maxLeafEntries) {
                splitLeaf(leaf);
            } else {
                // If the first key of a non-root leaf changed, its
                // separator in the parent may need to be updated.
                updateParentSeparatorsAfterFirstKeyChange(leaf);
            }

            return true;
        }

        /**
         * Prints the required insertion message.
         */
        void insertAndPrint(int key, int pointer) {
            if (insert(key, pointer)) {
                System.out.println("(" + key + ", " + pointer + ") inserted");
            } else {
                System.out.println(
                    "(" + key + ", " + pointer + ") not inserted. "
                    + key + " found."
                );
            }
        }

        /**
         * Split an overflowing leaf.
         *
         * With capacity 2d, an overflowed leaf has 2d+1 records.
         * The left leaf keeps d records and the right leaf gets d+1.
         *
         * The first key of the right leaf is copied into the parent.
         */
        private void splitLeaf(LeafNode leaf) {
            LeafNode right = new LeafNode();

            int splitPoint = d;

            while (leaf.keys.size() > splitPoint) {
                right.keys.add(leaf.keys.remove(splitPoint));
                right.pointers.add(leaf.pointers.remove(splitPoint));
            }

            // Link the leaves.
            right.next = leaf.next;
            right.previous = leaf;

            if (leaf.next != null) {
                leaf.next.previous = right;
            }

            leaf.next = right;

            right.parent = leaf.parent;

            int separator = right.firstKey();

            if (leaf.parent == null) {
                // The old leaf becomes child 0 of a new root.
                InternalNode newRoot = new InternalNode();
                newRoot.children.add(leaf);
                newRoot.children.add(right);
                newRoot.keys.add(separator);

                leaf.parent = newRoot;
                right.parent = newRoot;
                root = newRoot;
            } else {
                insertChildIntoParent(leaf, separator, right);
            }
        }

        /**
         * Inserts a new child and separator into an internal parent.
         */
        private void insertChildIntoParent(
                Node leftChild,
                int separator,
                Node rightChild) {

            InternalNode parent = leftChild.parent;

            int childIndex = parent.children.indexOf(leftChild);

            parent.children.add(childIndex + 1, rightChild);
            parent.keys.add(childIndex, separator);
            rightChild.parent = parent;

            if (parent.keys.size() > maxInternalKeys) {
                splitInternal(parent);
            }
        }

        /**
         * Split an overflowing internal node.
         *
         * If there are 2d+1 keys:
         *   - the middle key (index d) is promoted
         *   - left keeps d keys
         *   - right keeps d keys
         *
         * This is different from leaf splitting because the promoted
         * separator is removed from the internal node.
         */
        private void splitInternal(InternalNode node) {
            int middleIndex = d;
            int promotedKey = node.keys.get(middleIndex);

            InternalNode right = new InternalNode();

            // Keys after the promoted key move to the right node.
            for (int i = middleIndex + 1; i < node.keys.size(); i++) {
                right.keys.add(node.keys.get(i));
            }

            // Children after the first d+1 children move to the right.
            for (int i = middleIndex + 1; i < node.children.size(); i++) {
                Node child = node.children.get(i);
                right.children.add(child);
                child.parent = right;
            }

            // Remove moved keys from the left node.
            while (node.keys.size() > middleIndex) {
                node.keys.remove(node.keys.size() - 1);
            }

            // Remove moved children from the left node.
            while (node.children.size() > middleIndex + 1) {
                node.children.remove(node.children.size() - 1);
            }

            right.parent = node.parent;

            if (node.parent == null) {
                InternalNode newRoot = new InternalNode();

                newRoot.keys.add(promotedKey);
                newRoot.children.add(node);
                newRoot.children.add(right);

                node.parent = newRoot;
                right.parent = newRoot;
                root = newRoot;
            } else {
                insertChildIntoParent(node, promotedKey, right);
            }
        }

        /* ============================================================
         * DELETE
         * ============================================================ */

        /**
         * Deletes key and its pointer.
         */
        boolean delete(int key) {
            LeafNode leaf = findLeaf(key);
            int index = Collections.binarySearch(leaf.keys, key);

            if (index < 0) {
                return false;
            }

            leaf.keys.remove(index);
            leaf.pointers.remove(index);

            // Root leaf may be empty.
            if (leaf == root) {
                return true;
            }

            if (leaf.keys.size() < minLeafEntries()) {
                rebalanceLeafAfterDelete(leaf);
            } else {
                updateParentSeparatorsAfterFirstKeyChange(leaf);
            }

            return true;
        }

        /**
         * Prints the required deletion message.
         */
        void deleteAndPrint(int key) {
            if (delete(key)) {
                System.out.println(key + " deleted.");
            } else {
                System.out.println(key + " not found, not deleted.");
            }
        }

        private int minLeafEntries() {
            return d;
        }

        private int minInternalKeys() {
            return d;
        }

        /**
         * Rebalance an underfull leaf by borrowing from a sibling when
         * possible; otherwise merge with a sibling.
         */
        private void rebalanceLeafAfterDelete(LeafNode leaf) {
            InternalNode parent = leaf.parent;
            int childIndex = parent.children.indexOf(leaf);

            LeafNode leftSibling =
                childIndex > 0
                    ? (LeafNode) parent.children.get(childIndex - 1)
                    : null;

            LeafNode rightSibling =
                childIndex + 1 < parent.children.size()
                    ? (LeafNode) parent.children.get(childIndex + 1)
                    : null;

            // Borrow from left if it has more than the minimum.
            if (leftSibling != null
                    && leftSibling.keys.size() > minLeafEntries()) {

                int borrowedKey =
                    leftSibling.keys.remove(leftSibling.keys.size() - 1);
                int borrowedPointer =
                    leftSibling.pointers.remove(
                        leftSibling.pointers.size() - 1);

                leaf.keys.add(0, borrowedKey);
                leaf.pointers.add(0, borrowedPointer);

                // Parent separator for this leaf is its first key.
                parent.keys.set(childIndex - 1, leaf.firstKey());

                return;
            }

            // Borrow from right if it has more than the minimum.
            if (rightSibling != null
                    && rightSibling.keys.size() > minLeafEntries()) {

                int borrowedKey = rightSibling.keys.remove(0);
                int borrowedPointer = rightSibling.pointers.remove(0);

                leaf.keys.add(borrowedKey);
                leaf.pointers.add(borrowedPointer);

                // Parent separator for right sibling changes.
                parent.keys.set(childIndex, rightSibling.firstKey());

                return;
            }

            // If no sibling can lend, merge.
            if (leftSibling != null) {
                mergeLeafNodes(leftSibling, leaf);
            } else if (rightSibling != null) {
                mergeLeafNodes(leaf, rightSibling);
            }
        }

        /**
         * Merge right leaf into left leaf.
         */
        private void mergeLeafNodes(LeafNode left, LeafNode right) {
            left.keys.addAll(right.keys);
            left.pointers.addAll(right.pointers);

            left.next = right.next;

            if (right.next != null) {
                right.next.previous = left;
            }

            InternalNode parent = left.parent;
            int rightIndex = parent.children.indexOf(right);

            // Remove the separator before the right child.
            parent.children.remove(rightIndex);
            parent.keys.remove(rightIndex - 1);

            if (parent == root && parent.keys.isEmpty()) {
                root = left;
                left.parent = null;
                return;
            }

            if (parent != root && parent.keys.size() < minInternalKeys()) {
                rebalanceInternalAfterDelete(parent);
            } else {
                updateAllParentSeparators(parent);
            }
        }

        /**
         * Rebalance an underfull internal node.
         *
         * Borrowing:
         *   A separator from the parent and one child are rotated.
         *
         * Merging:
         *   The parent separator is brought down between the two nodes.
         */
        private void rebalanceInternalAfterDelete(InternalNode node) {
            if (node == root) {
                if (node.keys.isEmpty() && node.children.size() == 1) {
                    root = node.children.get(0);
                    root.parent = null;
                }
                return;
            }

            InternalNode parent = node.parent;
            int nodeIndex = parent.children.indexOf(node);

            InternalNode leftSibling =
                nodeIndex > 0
                    ? (InternalNode) parent.children.get(nodeIndex - 1)
                    : null;

            InternalNode rightSibling =
                nodeIndex + 1 < parent.children.size()
                    ? (InternalNode) parent.children.get(nodeIndex + 1)
                    : null;

            // Borrow from left.
            if (leftSibling != null
                    && leftSibling.keys.size() > minInternalKeys()) {

                Node borrowedChild =
                    leftSibling.children.remove(
                        leftSibling.children.size() - 1);

                int oldParentSeparator = parent.keys.get(nodeIndex - 1);

                int newParentSeparator =
                    leftSibling.keys.remove(
                        leftSibling.keys.size() - 1);

                node.children.add(0, borrowedChild);
                borrowedChild.parent = node;

                node.keys.add(0, oldParentSeparator);
                parent.keys.set(nodeIndex - 1, newParentSeparator);

                return;
            }

            // Borrow from right.
            if (rightSibling != null
                    && rightSibling.keys.size() > minInternalKeys()) {

                Node borrowedChild = rightSibling.children.remove(0);

                int oldParentSeparator = parent.keys.get(nodeIndex);

                int newParentSeparator =
                    rightSibling.keys.remove(0);

                node.children.add(borrowedChild);
                borrowedChild.parent = node;

                node.keys.add(oldParentSeparator);
                parent.keys.set(nodeIndex, newParentSeparator);

                return;
            }

            // Merge.
            if (leftSibling != null) {
                mergeInternalNodes(leftSibling, node, nodeIndex - 1);
            } else if (rightSibling != null) {
                mergeInternalNodes(node, rightSibling, nodeIndex);
            }
        }

        /**
         * Merge left and right internal nodes using the separator from
         * their parent.
         */
        private void mergeInternalNodes(
                InternalNode left,
                InternalNode right,
                int parentSeparatorIndex) {

            InternalNode parent = left.parent;

            int separator = parent.keys.get(parentSeparatorIndex);

            // Bring the parent separator down.
            left.keys.add(separator);

            // Move all right keys and children to left.
            left.keys.addAll(right.keys);

            for (Node child : right.children) {
                left.children.add(child);
                child.parent = left;
            }

            parent.keys.remove(parentSeparatorIndex);
            parent.children.remove(right);

            if (parent == root && parent.keys.isEmpty()) {
                root = left;
                left.parent = null;
                return;
            }

            if (parent != root
                    && parent.keys.size() < minInternalKeys()) {
                rebalanceInternalAfterDelete(parent);
            } else {
                updateAllParentSeparators(parent);
            }
        }

        /* ============================================================
         * RANGE SEARCH
         * ============================================================ */

        /**
         * Returns all key-pointer pairs whose keys are in [k1, k2].
         * Returns null when no records are found.
         */
        List<int[]> rangeSearch(int k1, int k2) {
            if (k1 > k2) {
                return null;
            }

            LeafNode leaf = findLeaf(k1);
            List<int[]> result = new ArrayList<>();

            while (leaf != null) {
                for (int i = 0; i < leaf.keys.size(); i++) {
                    int key = leaf.keys.get(i);

                    if (key > k2) {
                        return result.isEmpty() ? null : result;
                    }

                    if (key >= k1) {
                        result.add(
                            new int[] {
                                key, leaf.pointers.get(i)
                            }
                        );
                    }
                }

                leaf = leaf.next;
            }

            return result.isEmpty() ? null : result;
        }

        /**
         * Prints range-search output exactly in the requested style.
         */
        void rangeSearchAndPrint(int k1, int k2) {
            List<int[]> result = rangeSearch(k1, k2);

            if (result == null) {
                System.out.println(
                    "no records in the range [" + k1 + ", " + k2 + "]"
                );
                return;
            }

            System.out.println("found");

            for (int[] pair : result) {
                System.out.println("(" + pair[0] + ", " + pair[1] + ")");
            }
        }

        /* ============================================================
         * PRINT
         * ============================================================ */

        /**
         * Prints the B+ tree level by level.
         *
         * Internal node:
         *   [(10) (20)]
         *
         * Leaf node:
         *   [(10: 201340) (20: 409678)]
         */
        void printTree() {
            if (root == null) {
                return;
            }

            Deque<Node> queue = new ArrayDeque<>();
            queue.add(root);

            int level = 0;

            while (!queue.isEmpty()) {
                int nodesAtThisLevel = queue.size();

                StringBuilder line = new StringBuilder();
                line.append("Level ").append(level).append(": ");

                for (int i = 0; i < nodesAtThisLevel; i++) {
                    Node node = queue.remove();

                    line.append(nodeToString(node));

                    if (i < nodesAtThisLevel - 1) {
                        line.append(" ");
                    }

                    if (!node.isLeaf()) {
                        InternalNode internal = (InternalNode) node;

                        for (Node child : internal.children) {
                            queue.add(child);
                        }
                    }
                }

                System.out.println(line);
                level++;
            }
        }

        private String nodeToString(Node node) {
            StringBuilder sb = new StringBuilder();
            sb.append("[");

            if (node.isLeaf()) {
                LeafNode leaf = (LeafNode) node;

                for (int i = 0; i < leaf.keys.size(); i++) {
                    if (i > 0) {
                        sb.append(" ");
                    }

                    sb.append("(")
                      .append(leaf.keys.get(i))
                      .append(": ")
                      .append(leaf.pointers.get(i))
                      .append(")");
                }
            } else {
                InternalNode internal = (InternalNode) node;

                for (int i = 0; i < internal.keys.size(); i++) {
                    if (i > 0) {
                        sb.append(" ");
                    }

                    sb.append("(")
                      .append(internal.keys.get(i))
                      .append(")");
                }
            }

            sb.append("]");
            return sb.toString();
        }

        /* ============================================================
         * STATISTICS
         * ============================================================ */

        /**
         * Prints structural statistics.
         *
         * Disk read/write statistics were explicitly removed by the
         * updated assignment, so they are not reported.
         */
        void printStatistics() {
            int height = treeHeight();
            int totalNodes = countNodes(root);
            int totalKeys = countLeafKeys();

            System.out.println("Tree Height: " + height);
            System.out.println("Total Nodes: " + totalNodes);
            System.out.println("Total Keys: " + totalKeys);
        }

        /**
         * Height convention:
         *   A tree containing only a root leaf has height 1.
         */
        private int treeHeight() {
            int height = 1;
            Node current = root;

            while (!current.isLeaf()) {
                InternalNode internal = (InternalNode) current;
                current = internal.children.get(0);
                height++;
            }

            return height;
        }

        private int countNodes(Node node) {
            if (node.isLeaf()) {
                return 1;
            }

            InternalNode internal = (InternalNode) node;
            int count = 1;

            for (Node child : internal.children) {
                count += countNodes(child);
            }

            return count;
        }

        private int countLeafKeys() {
            int count = 0;
            LeafNode leaf = getFirstLeaf();

            while (leaf != null) {
                count += leaf.keys.size();
                leaf = leaf.next;
            }

            return count;
        }

        /* ============================================================
         * HELPER METHODS
         * ============================================================ */

        /**
         * Finds the leaf where a key belongs.
         */
        private LeafNode findLeaf(int key) {
            Node current = root;

            while (!current.isLeaf()) {
                InternalNode internal = (InternalNode) current;

                /*
                 * Separator semantics:
                 * keys[i] is the first key in child i+1.
                 *
                 * Therefore:
                 *   key < keys[i] -> child i
                 *   key >= keys[i] -> continue to the right
                 */
                int childIndex = 0;

                while (childIndex < internal.keys.size()
                        && key >= internal.keys.get(childIndex)) {
                    childIndex++;
                }

                current = internal.children.get(childIndex);
            }

            return (LeafNode) current;
        }

        private LeafNode getFirstLeaf() {
            Node current = root;

            while (!current.isLeaf()) {
                InternalNode internal = (InternalNode) current;
                current = internal.children.get(0);
            }

            return (LeafNode) current;
        }

        /**
         * If a leaf's first key changes, update the corresponding
         * separator in the parent and continue upward when needed.
         */
        private void updateParentSeparatorsAfterFirstKeyChange(
                LeafNode leaf) {

            if (leaf.parent == null || leaf.keys.isEmpty()) {
                return;
            }

            updateSeparatorForChild(leaf);
        }

        /**
         * Updates the separator immediately before a child, if that
         * child is not the first child of its parent.
         */
        private void updateSeparatorForChild(Node child) {
            InternalNode parent = child.parent;

            if (parent == null) {
                return;
            }

            int childIndex = parent.children.indexOf(child);

            if (childIndex > 0) {
                int newSeparator = child.firstKey();
                parent.keys.set(childIndex - 1, newSeparator);
            }

            // Parent's first key is determined by its second child,
            // so changes to child 0 do not directly create a separator.
            if (childIndex == 0 && parent.parent != null) {
                updateSeparatorForChild(parent);
            }
        }

        /**
         * Recomputes separators for a subtree's immediate parent.
         * This is used after structural deletion operations and keeps
         * the separator representation consistent.
         */
        private void updateAllParentSeparators(InternalNode parent) {
            for (int i = 0; i < parent.keys.size(); i++) {
                parent.keys.set(
                    i,
                    parent.children.get(i + 1).firstKey()
                );
            }

            if (parent.parent != null) {
                updateAllParentSeparatorsForAncestor(parent);
            }
        }

        private void updateAllParentSeparatorsForAncestor(Node node) {
            InternalNode parent = node.parent;

            if (parent == null) {
                return;
            }

            for (int i = 0; i < parent.keys.size(); i++) {
                parent.keys.set(
                    i,
                    parent.children.get(i + 1).firstKey()
                );
            }

            if (parent.parent != null) {
                updateAllParentSeparatorsForAncestor(parent);
            }
        }
    }

    /* ------------------------- Main program ------------------------- */

    public static void main(String[] args) {
        if (args.length != 2 || !args[0].equalsIgnoreCase("init")) {
            System.err.println("Usage: java bplus init <d>");
            return;
        }

        int d;

        try {
            d = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            System.err.println("d must be an integer.");
            return;
        }

        if (d < 1) {
            System.err.println("d must be at least 1.");
            return;
        }

        BPlusTree tree = new BPlusTree(d);

        try (BufferedReader reader =
                new BufferedReader(new InputStreamReader(System.in))) {

            String line;

            while ((line = reader.readLine()) != null) {
                line = line.trim();

                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                processCommand(tree, line);
            }

        } catch (IOException e) {
            System.err.println("Error reading commands: " + e.getMessage());
        }
    }

    /**
     * Parses one command line.
     */
    private static void processCommand(
            BPlusTree tree,
            String line) {

        String[] tokens = line.trim().split("\\s+");

        if (tokens.length == 0) {
            return;
        }

        String command = tokens[0].toUpperCase();

        try {
            switch (command) {

                case "INSERT":
                    requireArguments(tokens, 3);
                    int insertKey = Integer.parseInt(tokens[1]);
                    int pointer = Integer.parseInt(tokens[2]);
                    tree.insertAndPrint(insertKey, pointer);
                    break;

                case "DELETE":
                    requireArguments(tokens, 2);
                    int deleteKey = Integer.parseInt(tokens[1]);
                    tree.deleteAndPrint(deleteKey);
                    break;

                case "SEARCH":
                    requireArguments(tokens, 2);
                    int searchKey = Integer.parseInt(tokens[1]);
                    tree.searchAndPrint(searchKey);
                    break;

                case "RANGESEARCH":
                    requireArguments(tokens, 3);
                    int k1 = Integer.parseInt(tokens[1]);
                    int k2 = Integer.parseInt(tokens[2]);
                    tree.rangeSearchAndPrint(k1, k2);
                    break;

                case "PRINT":
                    if (tokens.length == 2
                            && tokens[1].equalsIgnoreCase("STATISTICS")) {
                        tree.printStatistics();
                    } else if (tokens.length == 1) {
                        tree.printTree();
                    } else {
                        throw new IllegalArgumentException(
                            "PRINT takes no arguments, except PRINT STATISTICS."
                        );
                    }
                    break;

                default:
                    System.err.println("Unknown command: " + line);
            }

        } catch (IllegalArgumentException e) {
            System.err.println("Invalid command: " + line);
        }
    }

    private static void requireArguments(
            String[] tokens,
            int expected) {

        if (tokens.length != expected) {
            throw new IllegalArgumentException(
                "Expected " + expected + " arguments."
            );
        }
    }
}

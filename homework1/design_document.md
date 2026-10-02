# B+ Tree Design Document

## 1. Data Structures

The implementation uses an in-memory B+ tree with two node types: leaf nodes
and internal nodes. Both types inherit from an abstract `Node` class that
contains a parent pointer.

A `LeafNode` contains two parallel lists: `keys` and `pointers`. The key at
position `i` is associated with the pointer at position `i`. Keys are kept in
ascending order. Each leaf also has `next` and `previous` references to other
leaf nodes. These links allow a range query to move sequentially through the
records without repeatedly traversing the tree from the root.

An `InternalNode` contains a sorted list of separator keys and a list of child
pointers. An internal node with `k` separator keys has `k + 1` children. The
separator convention used by the implementation is:

`keys[i] = first key stored in children[i + 1]`.

Thus, when searching for a key, a key smaller than a separator goes to the
child on its left, while a key greater than or equal to the separator goes to
the child on its right.

The updated assignment defines capacity using parameter `d`. A leaf may hold
at most `2d` records, and a non-root leaf must contain at least `d` records.
An internal node may contain at most `2d` keys and `2d + 1` children, and a
non-root internal node must contain at least `d` keys. The root is exempt from
the minimum occupancy requirement.

No page or disk structures are required because the updated assignment
specifies that the entire tree must remain in memory.

## 2. Overall Design

The program is contained in a single Java file, `bplus.java`. The `main`
method requires the startup form:

`java bplus init <d>`

The program creates an empty `BPlusTree` object and then reads commands from
standard input. This matches the testing method specified in the assignment,
for example:

`java bplus init 2 < test.txt`

The command processor recognizes `INSERT`, `DELETE`, `SEARCH`,
`RANGESEARCH`, `PRINT`, and `PRINT STATISTICS`.

The tree starts as a single empty leaf node. As records are inserted, the
tree grows upward through node splitting. As records are deleted, the tree
uses borrowing and merging to maintain the minimum occupancy requirements.

Because the implementation is entirely in memory, no `pagesize` argument,
index file, disk manager, persistence mechanism, or simulated I/O counter is
used.

## 3. Search and Range Search

For a point search, the tree is traversed from the root. At each internal
node, the separator keys determine which child should be followed. Once the
appropriate leaf is reached, the leaf's sorted key list is searched. If the
key exists, its corresponding pointer is returned; otherwise `-1` is
returned.

Range search first locates the leaf that could contain the lower bound. It
then scans the leaf's records and follows the `next` links to subsequent
leaves. Records are returned while their keys are within the inclusive range
`[k1, k2]`. This approach takes advantage of the linked-leaf structure that
is characteristic of B+ trees.

## 4. Insertion and Splitting

Insertion first locates the appropriate leaf. Duplicate keys are rejected
before modifying the tree.

If the leaf has room, the new key-pointer pair is inserted in sorted order.
If insertion causes the leaf to contain `2d + 1` records, it is split into
two leaves. The left leaf retains `d` records and the right leaf receives the
remaining `d + 1` records. The first key of the right leaf is inserted into
the parent as the separator.

If the overflowing leaf is the root, a new internal root is created with the
two leaves as children. Otherwise the new child and separator are inserted
into the existing parent.

An internal node can similarly overflow after receiving a new child. When an
internal node contains `2d + 1` keys, its middle key is promoted to its
parent. The remaining keys and children are divided between the left and
right internal nodes. If the old internal node was the root, a new root is
created.

This process can propagate upward until the tree becomes valid again.

## 5. Deletion and Rebalancing

Deletion first searches for the key. If it does not exist, no tree structure
is changed.

If the key exists, its key-pointer pair is removed from the leaf. If the leaf
still contains at least `d` records, deletion is complete except for any
necessary separator-key update.

If the leaf becomes underfull, the implementation first attempts to borrow a
record from the left sibling. If that sibling has no extra record, it attempts
to borrow from the right sibling. When a record is borrowed, the affected
parent separator is updated.

If neither sibling can lend a record, two neighboring leaves are merged.
The corresponding separator and child reference are then removed from the
parent.

An internal node that becomes underfull is handled using the same general
strategy. It first attempts to borrow a child through rotation with a
sibling. If borrowing is impossible, it merges with a sibling and brings the
appropriate parent separator down into the merged node.

If the root becomes empty and has only one child, that child becomes the new
root. This reduces the height of the tree.

## 6. Tree Printing and Statistics

`PRINT` uses breadth-first traversal. A queue stores nodes in level order.
Each level is printed on a separate line. Internal nodes show separator keys,
while leaves show `(key: pointer)` pairs.

For example:

```text
Level 0: [(10)]
Level 1: [(5: 201451)] [(10: 201340) (20: 409678)]
```

`PRINT STATISTICS` reports the tree height, number of nodes, and number of
stored keys. Height is defined as the number of levels, so an empty-key root
leaf initially has height 1.

The previous version of the assignment requested disk-read and disk-write
statistics. The updated requirements explicitly remove disk I/O and require
an entirely in-memory implementation. Therefore, this implementation does not
maintain or report simulated I/O statistics.

## 7. Generative AI Strategy

Generative AI was used as a programming assistant to help translate the B+
tree algorithms into Java and organize the implementation into modular
methods. Critical prompts focused on implementing a B+ tree with integer
keys/pointers, supporting insertion, deletion, splitting, merging, borrowing,
range search, level-order printing, and the assignment's exact command format.

The generated solution was adapted to the instructor's updated requirements.
In particular, the original disk-based requirements were removed and replaced
with the `d`-based in-memory capacity rules. The implementation was also
structured so that leaf capacity, internal-node capacity, minimum occupancy,
separator maintenance, and command parsing directly correspond to the
assignment specification.

The implementation should be tested with multiple insertion and deletion
sequences, especially cases that cause leaf splits, internal splits, sibling
borrowing, leaf merging, internal merging, and root reduction.

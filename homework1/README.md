# B+ Tree Assignment — Java

## 1. Overview

This implementation follows the updated assignment requirements.

The B+ tree is implemented entirely in memory. It does **not** create or read a
`bplus_index.dat` file, does not use a page-size parameter, and does not
preserve the tree after the Java program terminates.

The program accepts the node-capacity parameter `d` when it starts.

For a given `d`:

- Leaf maximum = `2d` key-pointer pairs.
- Leaf minimum = `d` key-pointer pairs, except for the root.
- Internal maximum = `2d` separator keys and `2d + 1` children.
- Internal minimum = `d` separator keys, except for the root.
- Keys are integers.
- Pointers are integers.
- Duplicate keys are rejected.

## 2. Files

The submission contains:

```text
bplus.java
README.md
design_document.md
```

## 3. Compile

From the directory containing `bplus.java`:

```bash
javac bplus.java
```

This produces:

```text
bplus.class
```

and the required inner-class files.

## 4. Run

For `d = 2`:

```bash
java bplus init 2 < test.txt
```

For example:

```bash
java bplus init 2 < test.txt
```

The `init` command creates an empty in-memory B+ tree. The input commands
are then processed in order.

There is intentionally no persistence between separate executions because
the updated assignment explicitly says the implementation should be
entirely in memory.

## 5. Input Commands

### INSERT

```text
INSERT 10 201340
```

Output:

```text
(10, 201340) inserted
```

If key 10 already exists:

```text
(10, 201340) not inserted. 10 found.
```

### SEARCH

```text
SEARCH 10
```

If found:

```text
10 found, point is 201340
```

If not found:

```text
10 not found
```

### DELETE

```text
DELETE 10
```

If found:

```text
10 deleted.
```

If not found:

```text
10 not found, not deleted.
```

### RANGESEARCH

```text
RANGESEARCH 10 100
```

If records exist:

```text
found
(10, 201340)
(20, 409678)
(90, 201451)
```

If no records exist:

```text
no records in the range [10, 100]
```

### PRINT

```text
PRINT
```

The tree is displayed level by level. Internal nodes display separator keys,
while leaf nodes display key-pointer pairs.

Example:

```text
Level 0: [(10)]
Level 1: [(5: 201451)] [(10: 201340) (20: 409678)]
```

### PRINT STATISTICS

```text
PRINT STATISTICS
```

Example:

```text
Tree Height: 2
Total Nodes: 3
Total Keys: 3
```

The updated assignment removed disk I/O requirements, so simulated disk-read
and disk-write statistics are not printed.

## 6. Example Test File

Create `test.txt`:

```text
INSERT 10 201340
INSERT 20 37658
PRINT STATISTICS
INSERT 5 56743
SEARCH 10
RANGESEARCH 5 20
PRINT STATISTICS
DELETE 10
SEARCH 10
PRINT
PRINT STATISTICS
```

Run:

```bash
java bplus init 2 < test.txt
```

## 7. Implementation Notes

### Node representation

There are two node types:

- `LeafNode`
  - sorted `keys`
  - matching sorted `pointers`
  - `next` leaf pointer
  - `previous` leaf pointer
  - parent pointer

- `InternalNode`
  - sorted separator `keys`
  - `children`
  - parent pointer

For an internal node:

```text
number of children = number of keys + 1
```

The separator at index `i` is the first key in child `i + 1`.

### Search

Search starts at the root and follows the appropriate child until reaching a
leaf. Java's binary search is used inside leaves.

### Insertion

A new record is inserted into its sorted leaf.

If the leaf exceeds `2d` records, it is split. The first key of the new right
leaf is copied into the parent.

If an internal node exceeds `2d` separator keys, it is split and its middle
separator is promoted to the parent. If the root splits, a new root is
created.

### Deletion

After deleting a record, an underfull leaf first attempts to borrow a record
from a sibling. If neither sibling can lend a record, the leaf is merged with
a sibling.

Internal nodes are rebalanced similarly by borrowing or merging. If the root
becomes empty and has one child, that child becomes the new root.

### Range search

Leaves are connected in sorted order using `next` pointers. Range search first
locates the leaf containing the lower bound and then follows the leaf chain
until the upper bound is exceeded.

### PRINT

A breadth-first traversal using a queue displays one tree level per line.

### Statistics

The program reports:

- tree height
- total number of nodes
- total number of keys

The assignment's updated version explicitly removes simulated disk I/O, so no
read/write counters are maintained.

## 8. Important Assumption

The implementation uses the following standard separator convention:

```text
Internal key[i] = first key of child[i + 1]
```

Therefore, while searching:

```text
key < separator -> go left
key >= separator -> go right
```

This convention is also maintained after insertion, deletion, borrowing, and
merging.

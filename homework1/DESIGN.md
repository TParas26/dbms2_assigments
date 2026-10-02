# Homework 1 Design Document - In-Memory B+ Tree

## 1. Overview
This project implements an in-memory B+ tree in Java. Integer keys and integer pointers are stored in leaf nodes. Internal nodes contain separator keys used to guide searches. Leaf nodes are linked together for efficient range searches.

The implementation uses parameter `d`: leaves contain at most `2d` key-pointer pairs, while internal nodes contain at most `2d` keys and `2d + 1` children. Non-root nodes are rebalanced to maintain the minimum occupancy.

No disk storage, page files, persistence, or simulated I/O are used.

## 2. Node Representation
`Node` is the abstract base class. `LeafNode` stores key-pointer entries plus `next` and `previous` leaf links. `InternalNode` stores separator keys and child references.

An internal separator is the minimum key in the child immediately to its right. During search, a key greater than or equal to a separator moves to the right child.

## 3. Search and Range Search
Point search traverses from the root to the appropriate leaf and scans the leaf for the requested key.

Range search first finds the leaf containing the lower bound and then follows the linked leaves until the upper bound is passed.

## 4. Insertion
Insertion places a new key-pointer pair in sorted order. Duplicate keys are rejected.

When a leaf exceeds `2d` entries, it is split and the first key of the new right leaf is inserted into the parent. Overfull internal nodes are split similarly, with the middle separator promoted to the parent. A root split creates a new root.

## 5. Deletion
Deletion removes the key from its leaf. If the leaf becomes underfull, the implementation first attempts redistribution from a sibling. If redistribution is not possible, nodes are merged and the corresponding parent separator is removed.

Internal underflow is handled through borrowing or merging. If the root has one child after rebalancing, that child becomes the new root.

## 6. Printing and Statistics
`PRINT` displays the tree level by level. `PRINT STATISTICS` reports tree height, node count, and key count.

## 7. Complexity
Point search, insertion, and deletion require traversal proportional to tree height, plus local split or rebalancing work. Range search traverses to the first relevant leaf and then scans the linked leaves containing the range.

## 8. Generative AI Disclosure
ChatGPT was used as a development aid for explaining B+ tree algorithms, reviewing implementation logic, and helping draft portions of the Java implementation and documentation. The student should review, test, and understand the final submission before submitting it.

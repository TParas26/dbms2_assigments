# Homework 1 - In-Memory B+ Tree

## Files
- `bplus.java` - complete Java implementation
- `README.md` - build and usage instructions
- `DESIGN.md` - design document
- `test.txt` - sample commands
- `.gitignore` - excludes compiled Java files

## Requirements
Java 8 or newer is recommended.

The implementation is entirely in memory. It does not use disk files, persistence, or simulated I/O.

For parameter `d`:
- Leaf maximum: `2d` key-pointer pairs.
- Internal maximum: `2d` keys and `2d + 1` children.
- Non-root nodes maintain at least `d` entries/keys after deletion rebalancing.

## Compile
`javac bplus.java`

## Run in Windows Command Prompt
`java bplus init 2 < test.txt`

## Run in PowerShell
`Get-Content test.txt | java bplus init 2`

## Supported commands
- `INSERT <key> <pointer>`
- `DELETE <key>`
- `SEARCH <key>`
- `RANGESEARCH <low> <high>`
- `PRINT`
- `PRINT STATISTICS`

`PRINT STATISTICS` reports tree height, node count, and key count. No simulated disk I/O statistics are included.

Compiled `.class` files are excluded by `.gitignore`.

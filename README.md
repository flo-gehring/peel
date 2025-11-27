# PEEL 🍌
> [!WARNING]
> Usual disclaimer: This is  only a hobby project and should not be used in any serious capacity.

PEEL is short for *printable, extendable expression language*.
The long term goal is to create a system that allows its users (e.g. you) to configure an expression language
that suits their needs (*extendable*) and which allows for a convenient way to access and display intermediate results and
calculations (*printable*).

## Goals / TODOs

For the MVP (0.1 Release)

* [ ] Have a "complete" (whatever that means) Programming Language
    * [ ] Do Arithmetic and Logic
    * [ ] Branching
    * [ ] Loops
    * [ ] Declare Variables (Lexical scoping)
    * [ ] Define Functions (First class)
    * [ ] A number of useful builtin functions
* [ ] Easy way to register function from the host
* [ ] Have a JSON format as output
* [ ] Good Testcoverage

### Further TODOs

* Control how output is emitted
* Improve Performance (maybe something like a bytecode machnine?)

# Concepts

## To Decide:

* Should Everything be an expression? Variable Declaration etc?
* Does peel need objects?
* How far should the configurability go?
    * Register new Operators? Overload Operators with new Types
    * Overload functions
    * Totally different grammar?
    * Adding new Primitives (e.g. Number Types with units?)
* 
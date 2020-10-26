# BlaBlaCar Technical Test

This technical test consists in implementing a given specification of a mower.
Without any technical requirement specified, I choose Scala to develop this application.

## Installation

Please install [sbt](https://www.scala-sbt.org/) if you want to compile and run the program from the command line.

With the [sbt assembly plugin](https://github.com/sbt/sbt-assembly), you will be able to generate a fat JAR for this project.
```bash
sbt assembly
```
Otherwise, you will find one already available at the root of the project.

## Usage

You can then execute the program with a simple Java command:

```bash
java -jar blablacar-mower.jar
```


## Description

A Mower has only three commands allowed:
 - turn Left
 - turn Right
 - move Forward

It also contains position coordinates (X, Y) and orientation (N, E, W, S)

The grid is a rectangular surface.

Given a coordinate (X, Y), North points to Y+1, and East to X+1

Mowers are programmed using an input file. Here is the given sample

```bash
5 5
1 2 N
LFLFLFLFF
3 3 E
FFRFFRFRRF

```

The first line indicates the top right corner's coordinates.

Then, each two consecutives lines represents:
 - the initial position of the mower
 - the moves that we want to assign to the mower

Multiple mowers should be processed simultaneously in order to speed up the overall execution time.

## Identification of underlying issues

This test is a multithreading concurrency problem. 
Since mowers should be executed in different threads, the main issue here is the resource sharing.

In other word, we need to take care of potential collision between different mowers handled by different threads on the grid.

It means that we will need to introduce the concept of turn, and move the mowers according to a specific ordering, that is the order of the mower's creation from the input file.

Therefore, mowers are stored in a Queue, as it perfectly matches the First-In First-Out ordering that we need.

There are several way to handle multithreading concurrency and resource sharing problems.

We can use the good old mutexes, synchronized operations, semaphores, etc...

Or we can also think of threads pools with the Executor Framework and CompletableFutures.

I choose a different approach for this test: [event-driven architecture](https://en.wikipedia.org/wiki/Event-driven_architecture) through the [Actor Model](https://en.wikipedia.org/wiki/Actor_model). 

## Event-driven architecture and Actor Model

There are multiple reasons that led me to choose this approach.
But before going into details, here is the [Wikipedia definition](https://en.wikipedia.org/wiki/Actor_model) of the Actor model:

```
The actor model in computer science is a mathematical model of concurrent computation that treats actor as the universal primitive of concurrent computation.
In response to a message it receives, an actor can: make local decisions, create more actors, send more messages, and determine how to respond to the next message received.
Actors may modify their own private state, but can only affect each other indirectly through messaging (removing the need for lock-based synchronization).
```

I choose to represent mowers with actors (MowerActor). They maintain their own local state, and communicate only with another specific actor which goal is to supervise the global orchestration of the moves processing (SupervisorActor).

The supervisor is the only Actor that handles the global state of the grid that contains the position of all mowers of the simulation.

I tried to use immutable variables as much as possible, excepted for the grid that I choose to define as a mutable variable (declared as a var instead of a val).

Immutability allows switching between states without introducing side effects.
Specifically, we store mowers into a Queue, but we are never modifying it.

Instead, if we need to update this queue, it means that we actually need to change state.

In that case, we switch state by passing as a parameter the new value of the updated queue, keeping the previous state's Queue unchanged.

The concept of state is fundamental here: each actor switches state according to the messages that it receives.

In the code, states are materialized by:
```
context.become(...)
```

A MowerActor always responds to the SupervisorActor, but never asks for something without being triggered by the Supervisor.
Since the global state is handled within the SupervisorActor, no MowerActor should have to send by itself a message.
This ensures that the SupervisorActor is always in charge of the state change.

Moreover, actors send asynchronous and immutable messages. And by introducing states, we limit the type of messages that an actor could receive.

An actor is never blocked. Instead of blocking a resource shared by multiple threads, this architecture is based on message events to modify data.
A thread never accesses directly a resource, the actor does.
It only sends messages to other actors mailboxes, and only certain type of messages implies a state change.

Therefore, actor internal structure is very light. Technically, we could have millions of actors.
The perfomance depends on the number of messages sent between actors and not on the number of actors.

So by defining consistent states, we removed the need to lock access to a shared resource, but also potential side effects.

## Optimization
This technical test has been implemented following TDD, until a certain level.
I struggle to test the SupervisorActor, since actors should not be tested according to their states, but in terms of behaviour. 
In other words, actor should be unit tested by the way they behave when receiving a specific message.
 And this specific actor is sending different messages that imply several state changes, making it harder for unit testing.

To read the input file, I use the recursion, and specifically tail recursion in order to avoid stack overflow during Runtime.

We could also add a Graphical User Interface instead of only the final position of the mowers.

Please note that the Akka framework that implements the Actor Model also exists in [Java](https://doc.akka.io/docs/akka/current/actors.html).

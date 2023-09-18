# The long story of the test structure Testaco tries to support
## A small user story

All the systems I have created through decades of development have been variations on a simple theme.
There is some kind of user, there is usually some kind of persistence, and there is some kind of data
processing going on between the user and the data store.

    This section will distil systems of hundreds of thousands of lines of code into one diagram. 
    It is going to seem very simple, and Testaco will therefore feel overly complex for such 
    a simple system. Please keep this in mind if you feel the complexity gets too much.
    The underlying goal is to reduce system or maintenance complexity, not add to it.

```mermaid
sequenceDiagram
actor U as User
participant F as Browser/App
participant B as Backend
participant DB
U->>F
F->>B
B->>DB
DB->>B
B->>DB
DB->>B
B->>F
F--U
```
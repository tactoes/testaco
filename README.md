# Testaco

Testaco is a re-implementation of a library from the early 2000s called [dbunit](https://www.dbunit.org/). My teams used 
dbunit to great success for more than a decade, but when I moved on from one of the teams after 2020 I
ended up taking a look at the source code and decided that the world has moved on since the 2000s, so I decided to re-implement the thing.

I wanted to get rid of all the factories, I wanted a tighter integration with Spring (because that is what my teams have 
been using, like it or not), and I wanted to rip out a lot of what I consider technologies I won't use again. I also
did not want to look another XML file in the face again if I could avoid it. If I could make it so that it is reusable
and understandable for others, that is good, but the itch I wanted to scratch was mostly my own. If you don't feel your
needs are met, give me a pull request or use dbunit.

## Why the need for this library?

Long story. The elevator pitch is basically with the number of vulnerabilities reported we need tests that also cover
behaviour embedded in libraries. If you use as many libraries you can you can expect about two vulnerability reports a
week for a system with a front- and backend. A lot of teams handle that by configuring a tool like 
[Dependencytrack](https://dependencytrack.org/), 
[Nexus Scanner](https://securityboulevard.com/2020/03/nexus-vulnerability-scanner-getting-started-with-vulnerability-analysis/), or
[DependencyCheck](https://github.com/jeremylong/DependencyCheck), and then promply ignore the results because their
test strategy is not equipped to handle the problem, which you 
[shouldn't](docs/supplychainwoes.md).

You can, of course, decide not to use libraries, but that carries its own bunch of problems, one of the biggest ones
being that having a piece of code used by a lot of people *will* root out more bugs. How will you configure your 
code base and teams to ensure that you find the two vulnerabilities per week? Your code is no less buggy than anyone 
else's. When it comes to security-sensitive libraries (authentication springs to mind) there are even 
[recommendations](https://owasp.org/www-project-proactive-controls/v3/en/c2-leverage-security-frameworks-libraries)
that you don't.

The long story is [here](docs/longstory-testing.md).

## What does it do?

Testaco forms the database checking part of an end-to-end testing framework that allows for 
[database refactoring](https://en.wikipedia.org/wiki/Database_refactoring). Being able to efficiently mutate the data
model of the system is a core concern if you want to have end-to-end tests, and failure to reach this goal is
probably the biggest reason why people don't test nonfunctional aspects of their systems, which they 
[should](docs/whynonfunctionaltests.md).

The long story about the test system I had in mind while writing the above is [here](docs/test-partition.md)

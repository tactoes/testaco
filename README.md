# Testaco

Testaco is a re-implementation of a library from the early 2000s called [dbunit](https://www.dbunit.org/). My teams used 
dbunit to great success for more than a decade, but when I moved on from one of the teams after 2020 I
ended up taking a look at the source code and decided that the world has moved on since the 2000s, so I decided to re-implement the thing.

I wanted to get rid of all the factories, I wanted a tighter integration with Spring (because that is what my teams have 
been using, like it or not), and I wanted to rip out a lot of what I consider technologies I won't use again. I also
did not want to look another XML file in the face again if I could avoid it. If I could make it so that it is reusable
and understandable for others, that is good, but the itch I wanted to scratch was mostly my own. If you don't feel your
needs are met, give me a pull request or use dbunit. 

I am unapologetically opinionated on the matter, and as a result, so is the library. I do however feel I have
reasons for my opinions. These opinions might already be or will become wrong. If you feel you have found an 
objective argument for why I am wrong I am most willing to listen, more so to people who can back their 
opinions with objective arguments or pull requests. I do ask that you sit down and sort through how much of 
your reactions are based in 

- not actually wanting to spend more time on testing because no effort will improve your situation
- technologies someone chose, and they are making testing hard
- the fact that you can't buy support for Testaco

If any of the above applies, this library won't help. You have issues in your technology base and/or your team that
needs to be sorted out first. If you attempt to add end-to-end testing to the mix before sorting those problems first
you will end up a worse place. Don't do it.

## Why the need for this library?

Long story. The elevator pitch is basically with the number of vulnerabilities reported for libraries in common use 
we need tests that also cover behaviour embedded in libraries. If you use as many libraries as possible you can 
expect about two vulnerability reports a week for a system with a front- and backend. A lot of teams handle 
that by configuring a tool like 
[Dependencytrack](https://dependencytrack.org/), 
[Nexus Scanner](https://securityboulevard.com/2020/03/nexus-vulnerability-scanner-getting-started-with-vulnerability-analysis/), or
[DependencyCheck](https://github.com/jeremylong/DependencyCheck), and then promply ignore the results because their
test strategy is not equipped to handle the workload, which you 
[shouldn't](docs/supplychainwoes.md). Some teams try to manage the problem using manual testing and/or scanning logs to
see if there is a functional regression, with predictable quality issues. Microservices just compound the issue since
behavioural regressions might not appear until two or three services run in conjunction - this most often happens along
the app/browser/backend boundary.

You can, of course, decide not to use libraries, but that carries its own bunch of problems, one of the biggest ones
being that having a piece of code used by a lot of people *will* root out more bugs. How will you configure your 
code base and teams to ensure that you find the two vulnerabilities per week? Your code is no less buggy than anyone 
else's. When it comes to security-sensitive libraries (authentication springs to mind) there are even 
[recommendations](https://owasp.org/www-project-proactive-controls/v3/en/c2-leverage-security-frameworks-libraries)
that you don't.

I strongly believe that keeping the libraries your system uses up-to-date should take so little time and require so
little skill that anyone on the team should be able to keep on top of the problem using the time it takes to drink their
first coffee of the day.

The long story is [here](docs/longstory-testing.md).

## What does it do?

Testaco forms the database checking part of an end-to-end testing framework that allows for 
[database refactoring](https://en.wikipedia.org/wiki/Database_refactoring). Being able to efficiently mutate the data
model of the system is a core concern if you want to have end-to-end tests and maintain them without great pain, 
and failure to reach this goal is
probably the biggest reason why people don't test nonfunctional aspects of their systems, which they 
[should](docs/whynonfunctionaltests.md).

The long story about the test system I had in mind while writing the above is [here](docs/test-partition.md)

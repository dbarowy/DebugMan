# AutoMan Visual Debugger README #

This repository is for the AutoMan Visual Debugger (AVD), a web interface designed to make debugging human-computational tasks easier.

This is a fork of the [original AutoMan Visual Debugger](https://bitbucket.org/btamaskar/automan-debugger) project, which was the subject of Bianca Tamaskar's independent study.

### What is the AutoMan Visual Debugger? ###

* This repository houses the two components of AVD: a 1) plugin implementation (an embedded web server built on [spray](http://spray.io/)) and 2) a JavaScript web UI.
* AVD is currently an alpha-quality research prototype.

### Prerequisites ###

It _almost_ goes without saying that you need a few build tools installed on your machine first, but having left these unmentioned before, I have been burned by irate programmers who claim that my software is "broken".  So, here we go.  You need:

1. Git (for AutoMan source)
2. Mercurial (for AVD source)
3. Simple Build Tool (aka SBT)

If you're on a Mac and you have [Homebrew](http://brew.sh/), all you need to do is type:

```
$ brew install git hg sbt
```

If you're not, see your local package manager documentation.

### Using AVD ###

In order to use AVD, you need to list it as a library dependency in your code and startup AutoMan with AVD as a plugin.  But before you do that, AVD should first be on your computer!

AVD will eventually be released as a JAR via Maven, which will simplify downloading and using it.  Until then, you will need to checkout AVD and AutoMan from their source code repositories, compile, then package the JAR "locally".  If your own code also uses SBT to manage the build, SBT is smart enough to check for the JAR locally to satisfy your program's dependency on AVD.  Otherwise, look for the AutoMan and AVD JARs in your `~/.ivy` directory.

Please ensure that code that depends on AVD uses the following library dependencies (*version numbers below are subject to change!*) in your `build.sbt`:

`"edu.umass.cs.plasma"  %%  "automandebugger" % "0.1-SNAPSHOT"`

You should probably also explicitly reference AutoMan:

`"edu.umass.cs"  %%  "automan" % "0.5-SNAPSHOT"`

### Setup ###

1. Checkout a copy of [AutoMan](https://github.com/dbarowy/AutoMan): `$ git clone git@github.com:dbarowy/AutoMan.git`
2. `$ cd AutoMan`
3. AutoMan presently needs the changes in the `webdebugger` branch: `$ git checkout webdebugger`
3. `$ sbt "project automan" publish-local`
4. `$ cd ..`
5. Checkout a copy of [DebugMan](https://github.com/dbarowy/DebugMan): `$ git clone git@github.com:dbarowy/DebugMan.git`
6. `$ cd DebugMan`
7. `$ sbt publish-local`

Your own code may now use locally-published AutoMan and AVD JARs.

### Test ###

AutoMan ships with a small suite of test applications.  We recommend using the `SimpleProgram` application to test AVD.

You can compile and run this application with a single command-line invocation.  First, go to your AutoMan directory,

```
$ cd AutoMan
```

and then run the following:

```
$ sbt "project SimpleProgram" "run -k [your AWS key] -s [your AWS secret]"
```

where the arguments to `-k` and `-s` are your Mechanical Turk credentials.

_Alternately_, you can also set up SBT to use `SimpleProgram` in an interactive way:

```
$ cd AutoMan
$ sbt
[info] Loading project definition from AutoMan/project
[info] Set current project to default-cf9e20 (in build file:AutoMan/)
> project SimpleProgram
[info] Set current project to simple_program (in build file:AutoMan/)
> compile
... lots of [info] stuff! ...
> run -k [your AWS key] -s [your AWS secret]
```

Regrettably, there's no simple way to stop SBT from `run`ning your programs without also killing SBT itself, since SBT launches tasks in the same JVM instance that it itself is running in.  Pressing `CTRL-C` will stop your program and also kill SBT.  You'll just need to follow the steps above (again) if you want to run `SimpleProgram` again.  If you find this to be truly annoying (as I do), either [see this Stack Overflow post](http://stackoverflow.com/questions/5137460/sbt-stop-run-without-exiting) or run from within an IDE like IntelliJ IDEA.

By default, `SimpleProgram` runs against the MTurk requester sandbox.

You'll know that AVD was loaded successfully if AutoMan prints something like the following to the console:

`[INFO] [12/03/2014 14:47:13.071] [on-spray-can-akka.actor.default-dispatcher-3] [akka://on-spray-can/user/IO-HTTP/listener-0] Bound to localhost/127.0.0.1:8080`

You should now be able to use the debugger by visiting [http://localhost:8080](http://localhost:8080) in your web browser.

Note AutoMan stores answers that it receives automatically.  This feature may complicate debugging.  You can clear the saved-answer database by running:

```
sbt memo-clean
```

Also, the `resetAccounts.sh` command available in the [http://aws.amazon.com/developertools/694](Mechanical Turk Command Line Tools) is also helpful for clearing HITs out of your account on MTurk's end of things, which is also a nice debugging aid.  See that page for setup instructions.


### Configuration ###

You need to tell AutoMan to use the AVD plugin in your code.  AutoMan requires a programmer to choose and initialize a "backend adapter" before calling human-computation functions.  These adapters tell AutoMan how to communicate with crowdsourcing services like Mechanical Turk.  You tell AutoMan that you'd like to use the AVD plugin during adapter initialization.  For example,

```
val a = MTurkAdapter { mt =>
    mt.plugins = List(AutomanDebugger.plugin)
    mt.access_key_id = [your AWS key]
    mt.secret_access_key = [your AWS secret]
}
```

`MTurkAdapter` is a subclass of `AutomanAdapter`.  All `AutomanAdapter`s have a `plugins` field, for which you can supply a `List[Plugin]`.

Note that, in addition to an import for the `MTurkAdapter`, you will need to import the `AutomanDebugger`:

```
import edu.umass.cs.automan.adapters.MTurk._
import edu.umass.cs.plasma.automandebugger.AutomanDebugger
```

### Developer guidelines ###

Modifying AVD requires you to use the following procedure:

1. Make a change in the AVD source code.
2. Ensure that the change compiles (e.g., run `sbt compile`).
3. Publish the changes locally: `cd DebugMan; sbt publish-local`.
4. Recompile and run your code.  If you're using `SimpleProgram` as a test as above, you can just `run` the code again; SBT will pickup the new changes.  Alternately, if you're using IntelliJ IDEA with the SBT and Scala plugins, you can just run your code again.

### Contributing ###

If you're not Dan, send me a Pull Request.

### Who do I talk to? ###

* If you find bugs, please open an Issue in this repository.
* AVD is currently maintained by Daniel Barowy.

### Acknowledgements ###

This material is based upon work supported by the National Science Foundation under Grant No. CCF-1144520.

# GradleCacheToMavenLocal
A Simple Tool made by AdrianTodt

### Okay, What the Fuck is This?

As you may know, I like Gradle a lot, but one of it's problems (AFAIK) is that what is cached by it can't be retrieved without a internet connection.

As I'm currently only have Internet Access at Saturdays (and some exceptions), this blocks me from dealing with dependencies AT ALL.

### What the Fuck it Does?

This tool, when executed in your User Folder (`C:\Users\USERNAME`, for example), converts your `.gradle/caches` into a `.m2/repository`, that by default is Gradle's `mavenLocal()`.

In sum, after some minutes of work, you get a Maven Repository with all cached dependencies.

### Does it have any incompatibilites?

I made this tool using my `.gradle` folder as reference, so I can't ensure anything at all.

If you have a `C:\Users\<USERNAME>\.gradle\caches\modules-<ANY>\files-<ANY>` folder, it's enough for it to process.

### How to Use?

- Grab the Jar (it should be on `Releases`)
- Download it into your User Folder (`C:\Users\<USERNAME>` or `~/`, depending on your OS)
- Run it (If you run using a Terminal, you'll see it running and what it is processing)

The tool does not have a GUI, nor a User prompt. It assumes leniently everything and try it's best to not explode.
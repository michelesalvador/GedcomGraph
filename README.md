# GedcomGraph

_GedcomGraph_ is a Java library that creates a graphic model of a genealogical tree on top of a [FamilySearch Gedcom](https://github.com/FamilySearch/Gedcom) object.

_GedcomGraph_ doesn't produce any kind of graphic output, but only the Java model of a genealogical tree on a cartesian plane, with the center on the top-left corner. To become a visible diagram, a further graphical implementation is needed, such a Java Canvas or an Android app.

_GedcomGraph_ has been primary created to generate the genealogical diagram into the Android app [Family Gem](https://github.com/michelesalvador/FamilyGem). But obviously it could be recycled for any other genealogical Java project that uses a FamilySearch Gedcom object.

You can find a simple implementation of _GedcomGraph_ made with Java AWT/Swing in [GedcomGraph canvas](https://github.com/michelesalvador/GedcomGraph-canvas).

_GedcomGraph_ is written in Java 1.7, so to be compatible with older versions of Android: it has been tested up to Android 4.4 KitKat.

_GedcomGraph_ can receive some option to modify the tree output, but basically the tree has always the zero coordinates on top-left corner, the ancestors above and the descendants below, and the marriage year between spouses.

## Options

At the moment you can choose to display:
- Number of generations of ancestors
- Number of generations of descendants
- ~~Number of generations of uncles~~
- ~~Siblings~~

## Package

_GedcomGraph_ is a Maven project written with Eclipse.

To generate the file `gedcom-graph-X.X.X.jar` you have to download the code and build the package in some way.

The package name is `graph.gedcom`, maybe not definitive. 😬

## Implementation

After added the jar file to your project, in short you have to:
1. Instantiate a graph.
2. Place the cards on a canvas.
3. Make the graph aware of the actual size of the cards.
4. Let the graph calculate the position of cards and lines.
5. Place the cards in their position.
6. Add the lines.

### Android

In Android you have to do these steps:

- Parse a Gedcom file (*.ged) with the FamilySearch Gedcom parser to produce a `org.folg.gedcom.model.Gedcom` object.
	```java
	File file = new File("myfamily.ged");	
	Gedcom gedcom = new ModelParser().parseGedcom(file);
	gedcom.createIndexes();
	```
- Create a `graph.gedcom.Graph` instance, fed with the Gedcom object.
	```java
	Graph graph = new Graph(gedcom);
- Pass to the graph some option, if you want.
	```java
	graph.maxAncestors(3).showFamily(1);
- Start the graph with the id of the fulcrum person.
	```java
	graph.startFrom("I1");
- Add the cards to the Android layout.<br>
Only after added to the layout it's possible to know the actual size of a card.
	```java
	(ToDo)
- Pass to the graph width and height of all the cards.
- Call `graph.arrange()` to make the graph calculate the position of all the cards.
- Update the position of the cards on the layout.
- Add the lines to the layout.

---

This project started on December 2019.<br>
Author is Michele Salvador, an italian self-taught programmer and genealogy enthusiast.

For questions, problems, suggestions please [open an issue](https://github.com/michelesalvador/GedcomGraph/issues).
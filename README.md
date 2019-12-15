# GedcomGraph

_GedcomGraph_ is a Java library that creates a graphic model of a genealogical tree on top of a [FamilySearch Gedcom](https://github.com/FamilySearch/Gedcom) object.

_GedcomGraph_ doesn't produce any kind of graphic output, but only the Java model of a genealogical tree on a cartesian plane, with the center on the top-left corner. To become a visible diagram, a further graphical implementation is needed, such a Java Canvas or an Android app.

_GedcomGraph_ has been primary created to generate the genealogical diagram into the Android app [Family Gem](https://github.com/michelesalvador/FamilyGem). But obviously it could be recycled for any other genealogical Java project that uses a FamilySearch Gedcom object.

~~You can find a simple implementation of _GedcomGraph_ made with Java AWT/Swing in [GedcomGraph Canvas](https://github.com/michelesalvador/GedcomGraph-Canvas)~~. NOT YET

_GedcomGraph_ is written in Java 1.7, so to be compatible with older version of Android: it has been tested up to Android 4.1 Jelly Bean.

_GedcomGraph_ can receive some option to modify the output tree, but basically the tree is always top-to-down, with marriage years between spouses, and the zero coordinates in the top-left corner of the area.

The package name `graph.gedcom` is maybe not definitive. 😬

## Options

At the moment you can choose to display:
- Number of generations of ancestors
- ~~Number of generations of uncles~~
- ~~Siblings~~
- ~~Number of generations of descendants~~

## Implementation

In short the steps are:
1. Create the graph model.
2. Make the graph cards aware of their actual size.
3. Let the graph calculate the position of the cards.
4. Place the cards in their correct position.
5. Add the lines.

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
- Pass to the graph the class of a concrete card that extends `graph.gedcom.Card`.
	```java
	public static class ConcreteCard extends Card {
		public ConcreteCard (Person person) {
			super(person);
		}
		...
	}
    
- Pass to the graph some other option, if you want.
- Start the graph with the id of the fulcrum person.
- Add the cards to the Android layout.<br>
Only after added to the layout it's possible to know the actual size of a card.
- Pass to the graph width and height of all the cards.
- Call `graph.arrange()` to make the graph calculate the position of all the cards.
- Update the position of the cards on the layout.
- Add the lines to the layout.

---

This project started on December 2019.<br>
Author is Michele Salvador, an italian self-taught programmer and genealogy enthusiast.

For questions, problems, suggestions please [open an issue](https://github.com/michelesalvador/GedcomGraph/issues).
# GedcomGraph

GedcomGraph is a Java library that creates a graphic model of a genealogical tree on top of a [Gedcom 5 Java](https://github.com/FamilySearch/gedcom5-java) object.

GedcomGraph doesn't produce any kind of graphic output, but only the Java model of a genealogical tree on a cartesian plane, with the center on the top-left corner.<br>
To become a visible diagram, a further graphical implementation is needed, such a Java Canvas or an Android app.

GedcomGraph has been primary created to generate the genealogical diagram into the Android app [Family Gem](https://github.com/michelesalvador/FamilyGem). But obviously it could be recycled for any other genealogical Java project that uses a Gedcom 5 Java object.

You can find a simple implementation of GedcomGraph made with Java AWT/Swing in [GedcomGraph Canvas](https://github.com/michelesalvador/GedcomGraph-Canvas).

GedcomGraph is written in Java 1.8, so to be compatible with older versions of Android: it has been tested down to Android 4.4 KitKat.

GedcomGraph can receive some option to modify the tree output, but basically the tree has always the zero coordinates on top-left corner, the ancestors above and the descendants below, and one single person as fulcrum.

## Options

You can choose to display:
- Number of generations of ancestors
- Number of generations of descendants
- Number of generations of uncles
- Siblings

## Package

GedcomGraph is a Maven project written with Eclipse.

To generate the file `gedcomgraph-1.3.1.jar` you have to download the code and build the package in some way.

The package name is `graph.gedcom`.

## Implementation

After added the jar file to your project, in short you have to:
1. Instantiate a graph.
2. Place the cards on a canvas (e.g. an Android Layout).
3. Make the graph aware of the actual size of each card.
4. Let the graph calculate the position of cards and lines.
5. Place the cards in their position on the canvas.
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
- Pass some option to the graph, if you want.
	```java
	graph.maxAncestors(3).maxUncles(2).displaySiblings(true).maxDescendants(3).showFamily(1);
- Start the graph passing one person that will be the fulcrum.
	```java
	graph.startFrom(gedcom.getPerson("I1"));
- Add the cards to the Android layout.<br>
Actually only after added to the layout it's possible to know the actual size of a card.
	```java
	for (Node node : graph.getNodes()) {
		if (node instanceof UnitNode)
			box.addView(new GraphicUnitNode(getContext(), (UnitNode)node));
	}
- Pass to the graph width and height of all the cards.
	```java
	for (int i = 0; i < box.getChildCount(); i++) {
		View nodeView = box.getChildAt(i);
		if (nodeView instanceof GraphicUnitNode) {
			GraphicUnitNode graphicUnitNode = (GraphicUnitNode) nodeView;
			for (int c = 0; c < graphicUnitNode.getChildCount(); c++) {
				View cardView = graphicUnitNode.getChildAt(c);
				if (cardView instanceof GraphicCard) {
					GraphicCard graphicCard = (GraphicCard) cardView;
					graphicCard.card.width = cardView.getWidth();
					graphicCard.card.height = cardView.getHeight();
				}
			}
		}
	}
- Call `graph.arrange()` to make the graph calculate the position of all the cards.
- Update the position of the cards on the layout.
	```java
	for (int i = 0; i < box.getChildCount(); i++) {
		View nodeView = box.getChildAt(i);
		if (nodeView instanceof GraphicUnitNode) {
			GraphicUnitNode graphicUnitNode = (GraphicUnitNode) nodeView;
			RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) graphicUnitNode.getLayoutParams();
			params.leftMargin = graphicUnitNode.unitNode.x;
			params.topMargin = graphicUnitNode.unitNode.y;
			graphicUnitNode.setLayoutParams( params );
		}
	}
- Finally add the lines to the layout.

You can find a complete Android implementation in [/michelesalvador/FamilyGem/.../Diagram.java](https://github.com/michelesalvador/FamilyGem/blob/master/app/src/main/java/app/familygem/Diagram.java).

---

This project started on December 2019.<br>
Author is Michele Salvador, an italian programmer and genealogy enthusiast.

For questions, problems, suggestions please [open an issue](https://github.com/michelesalvador/GedcomGraph/issues).
# GedcomGraph

GedcomGraph is a Java library that creates a graphic model of a genealogical tree on top of a [GEDCOM 5 Java](https://github.com/FamilySearch/gedcom5-java) object.

GedcomGraph doesn't produce any kind of graphic output, but only the Java model of a genealogical tree on a cartesian plane, with the center on the top-left corner.  
To become a visible diagram, a further graphical implementation is needed, such a Java Canvas or an Android app.

GedcomGraph has been primary created to generate the genealogical diagram into the Android app [Family Gem](https://github.com/michelesalvador/FamilyGem).

You can find a simple implementation of GedcomGraph made with Java AWT/Swing in [GedcomGraph Canvas](https://github.com/michelesalvador/GedcomGraph-Canvas).  
And a complete Android implementation in [FamilyGem/.../Diagram.java](https://github.com/michelesalvador/FamilyGem/blob/master/app/src/main/java/app/familygem/Diagram.java).

GedcomGraph is written in Java 1.8, so to be compatible with older versions of Android. It has been tested down to Android 4.4 KitKat.

GedcomGraph can receive some option to modify the tree output, but basically the tree has always the zero coordinates on top-left corner, the ancestors above and the descendants below, and one single person as fulcrum.

GedcomGraph is a Maven project written with Eclipse.  
The project started on December 2019.  
Author is Michele Salvador, an italian programmer and genealogy enthusiast.

For questions, problems, suggestions please [open an issue](https://github.com/michelesalvador/GedcomGraph/issues).

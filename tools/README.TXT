========================================
jDip Tools
========================================

jDip Tools are source-independent from the jDip code base.
Some tools may require jDip to compile; if so, that will
be specified by the tool documentation.

To build these tools, Java 1.4 or later and ANT 1.6 or later
is required.

There is a single, master build.xml file used by ant. By default,
the tools will be placed in the build/ directory.

If the "dist" target is selected, zip files with the tools and
appropriate documentation (suitable for end users) is created.

EACH TOOL HAS ITS OWN LICENSE, WHICH SHOULD BE IN THE /etc
DIRECTORY FOR THAT TOOL.

Overall Layout

(root)
  build.xml			[master build file]
  README.TXT		[this file]
  
  build/			[created by running ant]
  	*.jar			[.jar files created by build process]
  build/classes		[temporary .class files]
  build/dist		[distribution files]
  javadoc/			[created by running ant]
  
  (directory)		[one for each tool]
  	src/			[source code (.java), resources]
	etc/			[misc. resources, documents, etc.]
	lib/			[libraries required to build source]



# nlScript: Natural Language Scripting

The Natural Language Scripting (nlScript) library provides a framework for replacing graphical user interfaces (GUIs) with a unifiedscripting interface based on natural language.

It provides all the tools necessary for creating domain-specific languages with a natural English syntax for any application:
* Means to define custom lanugage sentences conveniently.
* Define for each language expression what should happen upon parsing it.
* A ready editor to be displayed to the user, equipped with autocompletion based on the defined language.
* Integrated parsing engine and evaluation environment.
* Tools for debugging the language.
* Integrated Error handling



## Installation
With Maven:
```
<groupId>io.github.nlscript</groupId>
<artifactId>nlScript</artifactId>
<version>0.1.0</version>
```



## Basic usage

The Natural Language Scripting framework offers a convenient way to define the sentences your interface should understand, and provides an auto-completion enabled text editor for users to enter their instructions. The following code snippet shows how to create a parser, how to define a pattern for a sentence for it to parse, and how to display the editor:
```java
// Create an instance to our backend
Preprocessing preprocessing = new Preprocessing();

// Create a parser
Parser parser = new Parser();

// Teach the parser a first sentence
parser.defineSentence(

    // The template of the sentence: any variable which is read from the user's input
    // is written in '{' and '}', and specified by a name, a type and optionally a quantifier
    "Apply Gaussian blurring with a standard deviation of {stddev:float} pixel(s).",

    // An evaluator, whose single function is called upon parsing the sentence defined above. It
    // receives a 'ParsedNode', which can be used to evaluate the variables:
    new Evaluator() {
        public Object evaluate(ParsedNode pn) {
            double stdDev = (double)pn.evaluate("stddev");
            preprocessing.gaussianBlur((float)stdDev);
            return null;
        }
    });

// Display an editor, to enter and run user input:
new ACEditor(parser).setVisible(true);
```
In this example we state that we expect a literal "Apply Gaussian blurring with a standard deviation of ", followed by a floating point number, which we name "stddev" for later reference, followed by the literal "pixel(s).".

The evaluator uses a `Preprocessing` object which could be implemented like this, using ImageJ as an underlying image processing library: [GitHub](https://github.com/nlScript/nlScript-tutorial-java/blob/main/src/main/java/de/nlScript/tutorial/preprocessing/Preprocessing.java)

This is how it looks like:

<div align="center">
  <a target="_blank" href="https://www.youtube.com/watch?v=4DSDNhj9uHQ"><img src="https://img.youtube.com/vi/4DSDNhj9uHQ/0.jpg" alt="Demo video"></a>
</div>

### More details:

* [The same application, as a step-by-step tutorial](https://nlscript.github.io/Bla)
* [Details how to define variables](https://nlscript.github.io/Bla/variables.html)
* [Built-in types apart from `float`](https://nlscript.github.io/Bla/#built-in-types)
* [More detail about custom types](https://nlscript.github.io/Bla/custom-types.html)



## Motivation
Graphical user interfaces can easily become complex and confusing as the number of user input parameters increases. This is particularly true if a workflow needs to be configured, where (i) each step has its own set of parameters, (ii) steps can occur in any order and (iii) steps can be repeated arbitrarily. Consider the configuration of an image pre-processing workflow, which consists of the following algorithms, each having its own set of parameters:
- Gaussian blurring (standard deviation)
- Median filtering (window radius)
- Background subtraction (window radius)
- Conversion to grayscale
- Intensity normalization

A traditional graphical user interface (GUI) could e.g. look like this:

![](https://nlscript.github.io/Bla/images/Screenshot-00.png)


where the user can activate the various algorithms and specify their parameters as necessary. This user interface however does not take into account that different algorithms could occur repeatedly, and it does not allow to change the order.

Using Natural Language Scripting, we want to implement a text-based interface which reads and executes text like:
```bash
Apply Gaussian blurring with a standard deviation of 3 pixel(s).
Subtract the background with a window readius of 30 pixel(s).
Apply Median filtering with a window radius of 1 pixel(s).
Normalize intensities.
Apply Gaussian blurring with a standard deviation of 1 pixel(s).
```



## Custom types
It is possible and also common to define custom types. We could e.g. define a type `filter-size` which consists of a floating point number and a unit (e.g. `pixel(s)`). In `defineSentence` we could then use `filter-size` as type for the standard deviation parameter:

```java
parser.defineType(
    // a name for the type:
    "filter-size",

    // a template string, similar to the one used in 'defineSentence':
    "{stddev:float} pixel(s)",

    // An evaluator, which evaluates this type upon parsing to a custom Java type
    // In this example, it will implicitly evaluate to a Double (because the type
    // of "stddev" is Double).
    new Evaluator() {
        public void evaluate(ParsedNode pn) {
            return pn.evaluate("stddev");
        }
    },

    // Use an optional third parameter to specify that auto-completion should
    // complete the entire type, and insert a placeholder for 'stddev'.
    true);

// Now use the type in a sentence:
parser.defineSentence(
    "Apply Gaussian blurring with a standard deviation of {stddev:filter-size}.",
    new Evaluator() {
        public Object evaluate(ParsedNode pn) {
            double stdDev = (double)pn.evaluate("stddev");
            preprocessing.gaussianBlur((float)stdDev);
            return null;
        }
    });
```
This will work like above, but auto-completion is improved and looks like:

![](https://nlscript.github.io/Bla/images/Screenshot-02.png)




## Finalizing

Now we can define the other sentences like this:

```java
parser.defineSentence(
    "Apply Median filtering with a window of radius {window-size:filter-size}.",
    new Evaluator() {
        public Object evaluate(ParsedNode pn) {
            int windowSize = (int)pn.evaluate("window-size");
            preprocessing.medianFilter(windowSize);
            return null;
        }
    });

parser.defineSentence(
    "Normalize intensities.",
    new Evaluator() {
        public Object evaluate(ParsedNode pn) {
            preprocessing.intensityNormalization();
            return null;
        }
    });

parser.defineSentence(
    "Subtract the background with a standard deviation of {window-size:filter-size}.",
    new Evaluator() {
        public Object evaluate(ParsedNode pn) {
            int windowSize = (int)pn.evaluate("window-size");
            preprocessing.subtractBackground(windowSize);
            return null;
        }
    });
```



## License

This project is licensed under the MIT License - see the [LICENSE.txt](LICENSE.txt) file for details.


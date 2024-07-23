<link href='https://fonts.googleapis.com/css?family=Caveat' rel='stylesheet'>
<link rel="stylesheet" href="css/github-markdown.css">
<link rel="stylesheet" href="css/projects.css">
<link rel="stylesheet" href="css//intellij-light.min.css">
<script src="js/highlight-11.6.0.min.js"></script>
<script>
function showDialog(id) {
  document.getElementById(id).showModal();
  document.documentElement.style.overflowY = 'hidden';
  return false; // to disable href
}

function hideDialog(id)  {
  document.getElementById(id).close();
  document.documentElement.style.overflowY = '';
  return false; // to disable href
}

function clickTab(e) {
  console.log(e);
  let tabSpan = e.target;
  let tabHeader = tabSpan.parentElement;
  let tabBody = tabHeader.nextElementSibling;
  console.log("tabBody");
  console.log(tabBody);
  for(let i = 0; i < tabHeader.children.length; i++) {
    if(tabSpan == tabHeader.children[i]) {
      console.log("Clicked tab " + i);
      tabHeader.children[i].classList.add('selected');
      tabBody.children[i].classList.add('selected');
    } else {
      console.log("Did not click tab " + i);
      tabHeader.children[i].classList.remove('selected');
      tabBody.children[i].classList.remove('selected');
    }
  }
}
</script>
<style>
.markdown-body h1 {
  font-family: 'Caveat';
  font-size: 40;
  background-color: #183d3d;
  color: white;
  padding: 40px;
}

.markdown-body h2 {
  margin-top: 3em;
}

.markdown-body img {
  margin: 50px;
}

.tab-header span {
  padding-left: 10px;
  padding-right: 10px;
  padding-bottom: 2px;
  border-left: 1px solid white;
  border-right: 1px solid white;
  cursor: pointer;
  color: white;
  background-color: rgb(70, 70, 70);
  border-top: none;
}

.tab-header span.selected {
  background-color: var(--color-canvas-subtle);
  color: black;
  border-top: 2px solid cornflowerblue;
}

.tab-content {
  display: none;
}

.tab-content.selected {
  display: block;
}

/*
table {
  width:100%;
}
table td {
  padding-top: 1em;
  padding-bottom: 1em;
}
*/

dialog {
  max-width: 800px;
  max-height: calc(100vh - 150px);
  overflow-y: auto;
  border-width: 0px;
  box-shadow: 0px 0px 15px;
}

dialog::backdrop {
  background-color: #000000a0;
}

details summary {
  display: block;
}

.content {
  background-color: #f5f5f5;
  margin: 1em;
  margin-right: 0px;
  padding: 10px;
  padding-bottom: 1px;
  font-size: smaller;
  border-radius: 5px;
}

@keyframes details-show {
  from {
    opacity:0;
    transform: var(--details-translate, translateY(-0.5em));
  }
}

details[open] > *:not(summary) {
  animation: details-show 150ms ease-in-out;
}

/*
table th:first-of-type {
  width:20%
}
table th:nth-of-type(2) {
  width:10%
}
table th:nth-of-type(3) {
  width:40%
}
table th:nth-of-type(4) {
  width:30%
}
*/

</style>


# Natural Language Scripting: A first tutorial<br> Image Pre-processing

## Why use a natural scripting interface
Graphical user interfaces can easily become complex and confusing as the number of user input parameters increases. This is particularly true if a workflow needs to be configured, where (i) each step has its own set of parameters, (ii) steps can occur in any order and (iii) steps can be repeated arbitrarily. Consider the configuration of an image pre-processing workflow, which consists of the following algorithms, each having its own set of parameters:
- Gaussian blurring (standard deviation)
- Median filtering (window radius)
- Background subtraction (window radius)
- Conversion to grayscale
- Intensity normalization

A traditional graphical user interface (GUI) could e.g. look like this:

![](images/Screenshot-00.png)

where the user can activate the various algorithms and specify their parameters as necessary. This user interface however does not take into account that different algorithms could occur repeatedly, and it does not allow to change the order.

Using Natural Language Scripting, we want to implement a text-based interface which reads and executes text like:
```bash
Apply Gaussian blurring with a standard deviation of 3 pixel(s).
Subtract the background with a window readius of 30 pixel(s).
Apply Median filtering with a window radius of 1 pixel(s).
Normalize intensities.
Apply Gaussian blurring with a standard deviation of 1 pixel(s).
```

## Create the backend
First of all, we'll implement a backend which does the actual processing. We therefore create a class that implements the actual algorithms. It uses Fiji as an underlying image processing library:
<details><summary><b>Preprocessing.java</b></summary>
<pre><code>import ij.IJ;
import ij.ImagePlus;
import ij.process.ImageProcessor;

public class Preprocessing {

	private ImagePlus image;

	public Preprocessing(ImagePlus image) {
		this.image = image;
	}

	public void gaussianBlur(float stdDev) {
		IJ.run(image, "Gaussian Blur...", "sigma=" + stdDev);
	}

	public void medianFilter(int radius) {
		IJ.run(image, "Median...", "radius=" + radius);
	}

	public void subtractBackground(float radius) {
		IJ.run(image, "Subtract Background...", "rolling=50");
	}

	public void convertToGray() {
		if(image.getType() == ImagePlus.COLOR_RGB)
			IJ.run(image, "8-bit", "");
	}

	public void intensityNormalization() {
		convertToGray();
		ImageProcessor ip = image.getProcessor();
		double min = ip.getMin();
		double max = ip.getMax();
		ip = ip.convertToFloat();
		ip.subtract(min);
		ip.multiply(1 / (max - min));
		image.setProcessor(ip);
	}
}
</code></pre>
</details>


## Implement an interface that understands a first sentence
The Natural Language Scripting framework offers a convenient way to define the sentences your interface should understand, and provides an auto-completion enabled text editor for users to enter their instructions. The following code snippet shows how to create a parser, how to define a pattern for a sentence for it to parse, and how to display the editor:
<div class="tab-header"><span>Java</span><span class="selected">Python</span><span>JavaScript</span></div>
<div class="tab-body">
<div class="tab-content selected">
```java
Parser parser = new Parser();
parser.defineSentence(
    "Apply Gaussian blurring with a standard deviation of {stddev:float} pixel(s).",
    null);

new ACEditor(parser).setVisible(true);
```
</div>
<div class="tab-content">
```python
parser = Parser();
parser.defineSentence(
    "Apply Gaussian blurring with a standard deviation of {stddev:float} pixel(s).",
    None);

ACEditor(parser).show();
```
</div>
<div class="tab-content">
```javascript
parser = nlScript.Parser();
parser.defineSentence(
    "Apply Gaussian blurring with a standard deviation of {stddev:float} pixel(s).",
    undefined);

new nlScript.ACEditor(parser, document.getElementById("nls-container"));
```
</div>
</div>

Find the full code here: [`Java`](https://github.com/nlScript/nlScript-tutorial-java/blob/main/src/main/java/nlScript/tutorial/preprocessing/Tutorial01.java) [`Python`](https://github.com/nlScript/nlScript-tutorial-python/blob/main/tutorial01.py) [`JavaScript`](https://github.com/nlScript/nlScript-tutorial-js/blob/main/docs/tutorial01.html).


In this example we state that we expect a literal "Apply Gaussian blurring with a standard deviation of ", followed by a floating point number, which we name "stddev" for later reference, followed by the literal "pixel(s).". There is a second parameter to `defineSentence()`, which we'll discover next.

The code snippet is sufficient to provide the means for user input, but nothing happens yet when the user clicks the `Run` button.

Find more information about <a href="variables.html" target="_blank">How to specify variables</a>.



## Evaluating the parsed text
The second argument to `parser.defineSentence()`, which was omitted above, is of type `Evaluator`. `Evaluator` is an interface with a single function
<div class="tab-header"><span>Java</span><span class="selected">Python</span><span>JavaScript</span></div>
<div class="tab-body">
<div class="tab-content selected">
```
interface Evaluator {
   Object evaluate(ParsedNode pn); 
}
```
</div>
<div class="tab-content">
```python
class IEvaluator(ABC):
    @abstractmethod
    def evaluate(self, pn: ParsedNode) -> object:
        pass


class Evaluator(IEvaluator):
    def __init__(self, evaluate: Callable[[ParsedNode], object]):
        self._evaluate = evaluate

    def evaluate(self, pn: ParsedNode) -> object:
        return self._evaluate(pn)
```
</div>
<div class="tab-content">
```javascript
export type Evaluator = (pn: ParsedNode) => any;
```
</div>
</div>

The task of the evaluator is to evaluate the expression (the sentence) we defined on the parser. In the example, it is responsible for the actual blurring. The argument to `evaluate()`, `pn`, is of type `ParsedNode`, which can be used to retrieve the parsed value for the standard deviation.

Variables in nlScript are defined hierarchically (see the paragraph "Custom types and type hierarchy" below). The result of parsing is also a hierarchical tree-like structure consisting of nodes of type `ParsedNode`. The `ParsedNode` has child `ParsedNode`s representing the variables the current type consists of. The sentence above consists of a string literal "Apply Gaussian blurring with a standard deviation of", a floating-point variable called "stddev", and a string literal "pixel(s).". Therefore, the `ParsedNode` given to the sentence's `Evaluator` has three child `ParsedNode`s, which can be accessed, e.g. by name, in the `Evaluator` and evaluated recursively.

<div class="tab-header"><span>Java</span><span class="selected">Python</span><span>JavaScript</span></div>
<div class="tab-body">
<div class="tab-content selected">
```java
ImagePlus image = IJ.openImage("http://imagej.net/images/clown.jpg");
Preprocessing preprocessing = new Preprocessing(image);
Parser parser = new Parser();
parser.defineSentence(
    "Apply Gaussian blurring with a standard deviation of {stddev:float} pixel(s).",
    pn -> {
        double stdDev = (double)pn.evaluate("stddev");
        preprocessing.gaussianBlur((float)stdDev);
        return null;
    });

new ACEditor(parser).setVisible(true);
```
</div>
<div class="tab-content">
```python
preprocessing = Preprocessing(None)
preprocessing.open('http://imagej.net/images/clown.jpg')
preprocessing.show()

parser = Parser()

def evaluateSentence(pn):
    stddev = pn.evaluate("stddev")
    preprocessing.gaussianBlur(stddev)

parser.defineSentence(
    "Apply Gaussian blurring with a standard deviation of {stddev:float} pixel(s).",
    evaluateSentence)

editor = ACEditor(parser)
editor.show()
```
</div>
<div class="tab-content">
```javascript
let preprocessing = new Preprocessing("output");

let parser = new nlScript.Parser();
parser.defineSentence(
  "Apply Gaussian blurring with a standard deviation of {stddev:float} pixel(s).",
  pn => {
    let stdDev = pn.evaluate("stddev");
    preprocessing.gaussianBlur(stdDev);
    preprocessing.show("output");
    return undefined;
  });

new nlScript.ACEditor(parser, document.getElementById("nls-container"));
```
</div>
</div>

Find the full code here: [`Java`](https://github.com/nlScript/nlScript-tutorial-java/blob/main/src/main/java/nlScript/tutorial/preprocessing/Tutorial02.java) [`Python`](https://github.com/nlScript/nlScript-tutorial-python/blob/main/tutorial02.py) [`JavaScript`](https://github.com/nlScript/nlScript-tutorial-js/blob/main/docs/tutorial02.html).

This is the first fully working example:

<video style="margin: auto; display: block;" width="800" controls>
  <source src="images/2023-09-07-13-11-46.cropped.mp4" type="video/mp4">
</video>




## Built-in types
Above, we specified the type of the standard deviation parameter `stddev` to be a floating point number, using `{stddev:float}`.

The following tables show other available built-in types:

<div>

<details>
<summary><code>int</code>: An integral number</summary>
<div class="content">
<p><em>Example:</em> <code>{page:int}</code> Defines a parameter 'page' as an integral number.</p>
<p><em>Parses e.g.: </em><code>5</code></p>
<p><em>Evaluates to: </em><b>Java:</b> <code>java.lang.Integer</code>, <b>Python:</b> <code>class 'int'</code>, <b>JavaScript:</b> <code>number</code></p>
</div>
</details>

<details>
<summary><code>float</code>: A floating-point number</summary>
<div class="content">
<p><em>Example: </em><code>{sigma:float}</code> Defines a parameter 'sigma' as a floating-point number.
<p><em>Parses e.g.: </em><code>5.3</code></p>
<p><em>Evaluates to: </em><b>Java:</b> <code>java.lang.Double</code>, <b>Python:</b> <code>class 'float'</code>, <b>JavaScript:</b> <code>number</code></p>
</div>
</details>

<details>
<summary><code>digit</code>: A character from '0' to '9'</summary>
<div class="content">
<p><em>Example: </em><code>{ch:digit}</code> Defines a parameter 'ch' as a digit.
<p><em>Parses e.g.: </em><code>3</code></p>
<p><em>Evaluates to: </em><b>Java:</b> <code>java.lang.Character</code>, <b>Python:</b> <code>class 'str'</code>, <b>JavaScript:</b> <code>string</code></p>
</div>
</details>

<details>
<summary><code>letter</code>: A character A-Z or a-z</summary>
<div class="content">
<p><em>Example: </em><code>{ch:letter}</code> Defines a parameter 'ch' as a letter.
<p><em>Parses e.g.: </em><code>b</code></p>
<p><em>Evaluates to: </em><b>Java:</b> <code>java.lang.Character</code>, <b>Python:</b> <code>class 'str'</code>, <b>JavaScript:</b> <code>string</code></p>
</div>
</details>

<details>
<summary><code>path</code>: A path within the local file system</summary>
<div class="content">
<p><em>Example: </em><code>{inputfile:path}</code> Defines a parameter 'inputfile' as a file system path.
<p><em>Parses e.g.: </em><code>'C:\Program Files'</code></p>
<p><em>Evaluates to: </em><b>Java:</b> <code>java.lang.String</code>, <b>Python:</b> <code>class 'str'</code>, <b>JavaScript:</b> <code>not implemented</code></p>
</div>
</details>

<details>
<summary><code>color</code>: A color</summary>
<div class="content">
<p><em>Example: </em><code>{text-color:color}</code> Defines a parameter 'text-color' as a color.
<p><em>Parses e.g.: </em><code>(25, 0, 233)</code> as an RGB value or one of the pre-defined colors below.</p>
<p><em>Evaluates to: </em>Integral number representing the RGB value of the color. <b>Java:</b> <code>int</code> (Conversion between <code>int</code> and <code>java.awt.Color</code> is possible using <code>new Color(int)</code> and <code>Color.toRGB()</code>) <b>Python:</b> <code>class 'int'</code> <b>JavaScript:</b> <code>number</code></p>

<p>Pre-defined colors:</p>
<ul>
<li><span style="background-color: rgb(0, 0, 0);       width: 1em; display: inline-block; height: 0.6em; border: black solid 1px;"></span>  black (0, 0, 0)
<li><span style="background-color: rgb(255, 255, 255); width: 1em; display: inline-block; height: 0.6em; border: black solid 1px;"></span>  white (255, 255, 255)
<li><span style="background-color: rgb(255, 0, 0);     width: 1em; display: inline-block; height: 0.6em; border: black solid 1px;"></span>  red (255, 0, 0)
<li><span style="background-color: rgb(255, 128, 0);   width: 1em; display: inline-block; height: 0.6em; border: black solid 1px;"></span>  orange (255, 128, 0)
<li><span style="background-color: rgb(255, 255, 0);   width: 1em; display: inline-block; height: 0.6em; border: black solid 1px;"></span>  yellow (255, 255, 0)
<li><span style="background-color: rgb(128, 255, 0);   width: 1em; display: inline-block; height: 0.6em; border: black solid 1px;"></span>  lawn green (128, 255, 0)
<li><span style="background-color: rgb(0, 255, 0);     width: 1em; display: inline-block; height: 0.6em; border: black solid 1px;"></span>  green (0, 255, 0)
<li><span style="background-color: rgb(0, 255, 180);   width: 1em; display: inline-block; height: 0.6em; border: black solid 1px;"></span>  spring green (0, 255, 180)
<li><span style="background-color: rgb(0, 255, 255);   width: 1em; display: inline-block; height: 0.6em; border: black solid 1px;"></span>  cyan (0, 255, 255)
<li><span style="background-color: rgb(0, 128, 255);   width: 1em; display: inline-block; height: 0.6em; border: black solid 1px;"></span>  azure (0, 128, 255)
<li><span style="background-color: rgb(0, 0, 255);     width: 1em; display: inline-block; height: 0.6em; border: black solid 1px;"></span>  blue (0, 0, 255)
<li><span style="background-color: rgb(128, 0, 255);   width: 1em; display: inline-block; height: 0.6em; border: black solid 1px;"></span>  violet (128, 0, 255)
<li><span style="background-color: rgb(255, 0, 255);   width: 1em; display: inline-block; height: 0.6em; border: black solid 1px;"></span>  magenta (255, 0, 255)
<li><span style="background-color: rgb(255, 0, 128);   width: 1em; display: inline-block; height: 0.6em; border: black solid 1px;"></span>  pink (255, 0, 128)
<li><span style="background-color: rgb(128, 128, 128); width: 1em; display: inline-block; height: 0.6em; border: black solid 1px;"></span>  gray (128, 128, 128)
</ul>
</div>
</details>

<details>
<summary><code>weekday</code>: Day of the week</summary>
<div class="content">
<p><em>Example: </em><code>{meeting-day:weekday}</code> Defines a parameter 'meeting-day' as a weekday.
<p><em>Parses e.g.: </em><code>Monday</code> one of the pre-defined weekdays below.</p>
<p><em>Evaluates to: </em>Integral number, starting with <code>0</code> for <code>Monday</code>. <b>Java:</b> <code>int</code> <b>Python:</b> <code>class 'int'</code> <b>JavaScript:</b> <code>number</code></p>
<p>Pre-defined weekdays:</p>
<ul>
<li>Monday (0)
<li>Tuesday (1)
<li>Wednesday (2)
<li>Thursday (3)
<li>Friday (4)
<li>Saturday (5)
<li>Sunday (6)
</ul>
</div>
</details>

<details>
<summary><code>month</code>: A month</summary>
<div class="content">
<p><em>Example: </em><code>{month-of-start:month}</code> Defines a parameter 'month-of-start' as a month.
<p><em>Parses e.g.: </em><code>January</code> one of the pre-defined months below.</p>
<p><em>Evaluates to: </em>Integral number, starting with <code>0</code> for <code>January</code>. <b>Java:</b> <code>int</code> <b>Python:</b> <code>class 'int'</code> <b>JavaScript:</b> <code>number</code></p>
<p>Pre-defined months:</p>
<ul>
<li>January (0)
<li>February (1)
<li>March (2)
<li>April (3)
<li>May (4)
<li>June (5)
<li>July (6)
<li>August (7)
<li>September (8)
<li>October (9)
<li>November (10)
<li>December (11)
</ul>
</div>
</details>

<details>
<summary><code>date</code>: A date</summary>
<div class="content">
<p><em>Example: </em><code>{date-of-birth:date}</code> Defines a parameter 'date-of-birth' as a date.
<p><em>Parses e.g.: </em><code>01 January 2020</code></p>
<p><em>Evaluates to: </em><b>Java:</b> <code>java.time.LocalDate</code> <b>Python:</b> <code>class 'datetime.date'</code> <b>JavaScript:</b> <code>Date</code></p>
</div>
</details>

<details>
<summary><code>time</code>: A time</summary>
<div class="content">
<p><em>Example: </em><code>{alarm:time}</code> Defines a parameter 'alarm' as a time.
<p><em>Parses e.g.: </em><code>6:30</code></p>
<p><em>Evaluates to: </em><b>Java:</b> <code>java.time.LocalTime</code> <b>Python:</b> <code>class 'datetime.time'</code> <b>JavaScript:</b> <code>Date</code></p>
</div>
</details>

<details>
<summary><code>datetime</code>: A date and time</summary>
<div class="content">
<p><em>Example: </em><code>{meeting-start:datetime}</code> Defines a parameter 'meeting-start' as a date and time.
<p><em>Parses e.g.: </em><code>01 January 2020 14:30</code></p>
<p><em>Evaluates to: </em><b>Java:</b> <code>java.time.LocalDateTime</code> <b>Python:</b> <code>class 'datetime.datetime'</code> <b>JavaScript:</b> <code>Date</code></p>
</div>
</details>

<details>
<summary><code>tuple&lt;type,n1,n2,...&gt;</code>: An n-dimensional tuple of comma-separated elements surrounded by <code>(</code>, <code>)</code></summary>
<div class="content">
<p><em>Example: </em><code>{point:tuple&lt;int,x,y&gt;}</code> Defines a parameter 'point' as a 2-tuple with entries named 'x' and 'y', the type of which is <code>int</code></p>
<p><em>Parses e.g.: </em><code>(15, 30)</code></p>
<p><em>Evaluates to: </em><b>Java:</b> <code>java.lang.Object[]</code>. The actual type of each entry depends on <code>type</code> (In the example, the type is <code>int</code>, so each entry in the array is a <code>java.lang.Integer</code>).<b>Python:</b> <code>class 'list'</code> <b>JavaScript:</b> <code>Array</code></p>
<p><code>type</code> can also be a custom-defined type (see below)</p>
</div>
</details>

<details>
<summary><code>list&lt;type&gt;</code>: An (unbound) comma-separated list</summary>
<div class="content">
<p><em>Example: </em><code>{colors:list&lt;color&gt;}</code> Defines a parameter 'colors' as a list with entries of type <code>color</code>.</p>
<p><em>Parses e.g.: </em><code>green, blue, yellow, (255, 30, 20)</code></p>
<p><em>Evaluates to: </em><b>Java:</b> <code>java.lang.Object[]</code>. The actual type of each entry depends on <code>type</code> (In the example, the type is <code>color</code>, so each entry in the array is a <code>java.lang.Integer</code>)<b>Python:</b> <code>class 'list'</code> <b>JavaScript:</b> <code>Array</code></p>
<p><code>type</code> can also be a custom-defined type (see below)</p>

<p>Furthermore, the cardinality of a list can be constrained:<p>
<ul>
<li><code>{colors:list&lt;color&gt;:5}</code> accepts only lists with exactly 5 colors
<li><code>{colors:list&lt;color&gt;:3-5}</code> accepts only lists with 3 to 5 colors
<li><code>{colors:list&lt;color&gt;:\*}</code> accepts lists with 0 to infinity colors
<li><code>{colors:list&lt;color&gt;:+}</code> accepts lists with 1 to infinity colors
<li><code>{colors:list&lt;color&gt;:?}</code> accepts lists with 0 or 1 colors
</ul>
</div>
</details>

<details>
<summary>Character class like <code>[a-zA-Z]</code>: A definable character</summary>
<div class="content">
<p><em>Example: (1) </em><code>{ch1:[a-z0-9]}</code> or (2) <code>{ch2:[^0-9]}</code></p>
<p><em>Parses: </em> (1) Any lower-case letter or digit and (2) any character that is not a digit</p>
<p>Evaluates to: <b>Java:</b> <code>java.lang.Character</code>. <b>Python:</b> <code>class 'str'</code> <b>JavaScript:</b> <code>string</code>. If a quantifier is used, it evaluates to <code>java.lang.Object[]</code> (<b>Java</b>), where each entry is of type <code>java.lang.Character</code>, <code>class 'list'</code> (<b>Python</b>) or <code>Array</code> (<b>JavaScript</b>). If you want to get the parsed string instead, you can use <code>ParsedNode.getParsedString()</code> instead of <code>ParsedNode.evaluate()</code>. </p>
<p>Some more examples:</p>
<ul>
<li><code>[a-zAB567]</code> matches a character 'a' - 'z', 'A', 'B', '5', '6' or '7'.
<li><code>[^1-35]</code> matches any character which is not '1', '2', '3' or '5'.
</ul>

<p>Furthermore, character classes can be extended to specify strings, by specifying the number of characters to match. In this case, it evaluates to <code>java.lang.String</code>.</p>
<ul>
<li><code>{identifier:[a-z]:5}</code> accepts a string consisting of 5 lower-case letters.
<li><code>{identifier:[a-z]:3-5}</code> accepts a string consisting of 3-5 lower-case letters.
<li><code>{identifier:[a-z]:\*}</code> accepts a string consisting of 0 to infinity lower-case letters.
<li><code>{identifier:[a-z]:+}</code> accepts a string consisting of 1 to infinity lower-case letters.
<li><code>{identifier:[a-z]:?}</code> accepts a string consisting of 0 or 1 lower-case letters.
</ul>

</div>
</details>

</div>




## Custom types and type hierarchy
<a href="custom-types.html" target="_blank">Details about custom types</a>

It is possible and also common to define custom types. We could e.g. define a type `filter-size` which consists of a floating point number and a unit (e.g. `pixel(s)`). In `defineSentence` we could then use `filter-size` as type for the standard deviation parameter:

<div class="tab-header"><span>Java</span><span class="selected">Python</span><span>JavaScript</span></div>
<div class="tab-body">
<div class="tab-content selected">
```java
parser.defineType(
    "filter-size",
    "{stddev:float} pixel(s)",
    pn -> pn.evaluate("stddev"));

parser.defineSentence(
    "Apply Gaussian blurring with a standard deviation of {stddev:filter-size}.",
    pn -> {
        double stdDev = (double)pn.evaluate("stddev");
        preprocessing.gaussianBlur((float)stdDev);
        return null;
    });
```
</div>
<div class="tab-content">
```python
def evaluateFilterSize(pn):
    return pn.evaluate("stddev")

# Create a custom type 'filter-size'
parser.defineType(
    "filter-size",
    "{stddev:float} pixel(s)",
    evaluator=evaluateFilterSize)

def evaluateSentence(pn):
    stddev = pn.evaluate("stddev")
    preprocessing.gaussianBlur(stddev)

parser.defineSentence(
    "Apply Gaussian blurring with a standard deviation of {stddev:filter-size}.",
    evaluator=evaluateSentence)
```
</div>
<div class="tab-content">
```javascript
parser.defineType(
  "filter-size",
  "{stddev:float} pixel(s)",
  pn => pn.evaluate("stddev"));

parser.defineSentence(
  "Apply Gaussian blurring with a standard deviation of {stddev:filter-size}.",
  pn => {
    let stdDev = pn.evaluate("stddev");
    preprocessing.gaussianBlur(stdDev);
    preprocessing.show("output");
    return undefined;
  });
```
</div>
</div>

Find the full code here: [`Java`](https://github.com/nlScript/nlScript-tutorial-java/blob/main/src/main/java/nlScript/tutorial/preprocessing/Tutorial03.java) [`Python`](https://github.com/nlScript/nlScript-tutorial-python/blob/main/tutorial03.py) [`JavaScript`](https://github.com/nlScript/nlScript-tutorial-js/blob/main/docs/tutorial03.html).

Autocompletion and evaluation will work as before, but the type `filter-size` can be re-used, e.g. for other filters like the median filter.

Custom types can be defined using built-in types and other custom types. In this way, an entire hierarchy of types can be built up. Here are more <a href="type-hierarchy.html" target="_blank">Details about type hierarchy</a>.






## Fine-tuning autocompletion: Parameterized autocompletion
Defining custom types has an additional advantage: It allows to customize autocompletion. In the example above, with default autocompletion in place, autocompletion looks like

![](images/Screenshot-01.png)

The user sees that a value for the `stddev` parameter is required, but doesn't know in which units the value needs to be entered. Usage is much clearer if we use inline, or parameterized autocompletion:

![](images/Screenshot-02.png)

This is accomplished by specifying a 4th parameter to `defineType`, a boolean that specifies whether to use parameterized autocompletion or not (`false` is the default):

<div class="tab-header"><span>Java</span><span class="selected">Python</span><span>JavaScript</span></div>
<div class="tab-body">
<div class="tab-content selected">
```java
parser.defineType("filter-size",
                  "{stddev:float} pixel(s)",
                  pn -> pn.evaluate("stddev"),
                  true);
```
</div>
<div class="tab-content">
```python
parser.defineType("filter-size",
                  "{stddev:float} pixel(s)",
                  lambda pn: pn.evaluate("stddev"),
                  True);
```
</div>
<div class="tab-content">
```javascript
parser.defineType("filter-size",
                  "{stddev:float} pixel(s)",
                  pn => pn.evaluate("stddev"),
                  true);
```
</div>
</div>
Find the full code here: [`Java`](https://github.com/nlScript/nlScript-tutorial-java/blob/main/src/main/java/nlScript/tutorial/preprocessing/Tutorial04.java) [`Python`](https://github.com/nlScript/nlScript-tutorial-python/blob/main/tutorial04.py) [`JavaScript`](https://github.com/nlScript/nlScript-tutorial-js/blob/main/docs/tutorial04.html).



## Multiple-choice autocompletion: choose a unit for `filter-size`

Microscope images commonly have a calibrated pixel size, and it is beneficial to specify algorithm parameters in real-world units instead of pixels. This facilitates the re-use of processing workflows, even if images were acquired at different resolutions. To extend the script in this regard, we define another type `units`. To take different units into account, we can re-define `units`. Here we will define it as `pixel(s)` and `calibrated units` (i.e. the units in which the image is calibrated). If the user selects the first option, we let the `units` type evaluate to `false`, otherwise to `true`.

<div class="tab-header"><span>Java</span><span class="selected">Python</span><span>JavaScript</span></div>
<div class="tab-body">
<div class="tab-content selected">
```java
parser.defineType("units", "pixel(s)", pn -> false);
parser.defineType("units", "calibrated units", pn -> true);
```
</div>
<div class="tab-content">
```python
parser.defineType("units", "pixel(s)", lambda pn:  false)
parser.defineType("units", "calibrated units", lambda pn: true)
```
</div>
<div class="tab-content">
```javascript
parser.defineType("units", "pixel(s)", pn => false);
parser.defineType("units", "calibrated units", pn => true);
```
</div>
</div>

The definition of `filter-size` now becomes
<div class="tab-header"><span>Java</span><span class="selected">Python</span><span>JavaScript</span></div>
<div class="tab-body">
<div class="tab-content selected">
```java
parser.defineType("filter-size", "{stddev:float} {units:units}", pn -> {
    double stddev = (Double) pn.evaluate("stddev");
    boolean units = (Boolean) pn.evaluate("units");
    if(units)
        stddev /= image.getCalibration().pixelWidth;
    return stddev;
}, true);
```
</div>
<div class="tab-content">
```python
def evaluateFilterSize(pn):
    stddev = pn.evaluate("stddev")
    units = pn.evaluate("units")
    if units:
        stddev /= preprocessing.getPixelWidth()
    return stddev


parser.defineType("filter-size",
                  "{stddev:float} {units:units}",
                  evaluateFilterSize,
                  True);
```
</div>
<div class="tab-content">
```javascript
parser.defineType("filter-size", "{stddev:float} {units:units}", pn -> {
    let stddev = pn.evaluate("stddev");
    let units = pn.evaluate("units");
    if(units)
        stddev /= preprocessing.getPixelWidth();
    return stddev;
}, true);
```
</div>
</div>

Find the full code here: [`Java`](https://github.com/nlScript/nlScript-tutorial-java/blob/main/src/main/java/nlScript/tutorial/preprocessing/Tutorial05.java) [`Python`](https://github.com/nlScript/nlScript-tutorial-python/blob/main/tutorial05.py) [`JavaScript`](https://github.com/nlScript/nlScript-tutorial-js/blob/main/docs/tutorial05.html).


In `evaluate()` we use `pn` to retrieve the values for `stddev` and `units`. If `units` evaluates to `true` (indicating that the user chose `calibrated units`), we calculate the filter-size in pixels, dividing by the pixel size.

Autocompletion will now look as follows:
![](images/Screenshot-03.png)
Then, after typing `5`, followed by `tab`:
![](images/Screenshot-04.png)



## Dynamic autocompletion at runtime using a custom `Autocompleter`

The completion option `calibrated units` obviously does not read very nice. Preferably we would replace it with the actual units the image was calibrated with, e.g. microns. This is runtime dependent, i.e. it depends on the image on which the script is executed. In the code example above, the image is opened before creating the language, but in a more realistic setting, we would like to run our script on any user-selected image.

To replace `calibrated units` with the real units, we use a custom `Autocompleter`, which needs to implement the `Autocompleter` interface:

<div class="tab-header"><span>Java</span><span class="selected">Python</span><span>JavaScript</span></div>
<div class="tab-body">
<div class="tab-content selected">
```java
public interface Autocompleter {
    Autocompletion[] getAutocompletion(ParsedNode pn, boolean justCheck);
}
```
</div>
<div class="tab-content">
```python
class IAutocompleter(ABC):
    @abstractmethod
    def getAutocompletion(self, pn: DefaultParsedNode, justCheck: bool) -> List[Autocompletion] or None:
        pass


class Autocompleter(IAutocompleter):
    def __init__(self, getAutocompletion: Callable[[ParsedNode, bool], List[Autocompletion]]):
        self._getAutocompletion = getAutocompletion

    def getAutocompletion(self, pn: ParsedNode, justCheck: bool) -> List[Autocompletion] or None:
        return self._getAutocompletion(pn, justCheck)
```
</div>
<div class="tab-content">
```javascript
interface Autocompleter {
    getAutocompletion(n: DefaultParsedNode, justCheck: boolean): Autocompletion[] | undefined;
}
```
</div>
</div>

`getAutocompletion()` returns an array of possible completions. As arguments, it takes a `ParsedNode`, which can be queried for what's already entered (`pn.getParsedString()`). A second parameter `justCheck` indicates that the actual completions are not needed and the function should just return whether it does autocomplete itself (`return Autocompletion.doesAutocomplete()`) or it leaves autocompletion to its children (`return null`). This is useful if the calculation of possible completion options is computationally expensive. Normally, `getAutocompletion()` returns an array of `Autocompletion`s. There are different subclasses for `Autocompletion`: `Autocompletion.Literal` for string constants, `Autocompletion.Parameterized` for a (single) parametric completion, `Autocompletion.EntireSequence` for a sequence of literal and parameterized completions, `Autocompletion.Veto`, which is a special completion which, if present in the list of options, prohibits autocompletion totally. There are several convenience functions to create the different kinds of autocompletions in the `Autocompletion` class.

In the example at hand, instead of multiple definitions of the type `units`, we define `units` to be an arbitrary string that may contain characters `a`-`z`, `A`-`Z`, `(` and `)`. Then, we use a custom `Autocompleter` as a 4th parameter to `defineType`, which returns as possible autocompletions `pixel(s)` and the actual string representing the units the open image is calibrated in (`imageUnits`).

<div class="tab-header"><span>Java</span><span class="selected">Python</span><span>JavaScript</span></div>
<div class="tab-body">
<div class="tab-content selected">
```java
parser.defineType(
    "units",                                                                // the type name
    "{unitstring:[a-zA-Z()]:+}",                                            // the pattern to parse
    pn -> !pn.getParsedString().equals("pixel(s)"),                         // the evaluator
    (pn, justCheck) -> Autocompletion.literal(pn, "pixel(s)", imageUnits)); // the autocompleter
```
</div>
<div class="tab-content">
```python
def getAutocompletion(pn, justCheck):
    return Autocompletion.literal(pn, "pixel(s)", imageUnits)

parser.defineType(
    "units",                                                                // the type name
    "{unitstring:[a-zA-Z()]:+}",                                            // the pattern to parse
    lambda pn: pn.getParsedString() != "pixel(s)",                          // the evaluator
    getAutocompletion)                                                      // the autocompleter
```
</div>
<div class="tab-content">
```javascript
parser.defineType(
    "units",                                                                // the type name
    "{unitstring:[a-zA-Z()]:+}",                                            // the pattern to parse
    pn => pn.getParsedString() !== "pixel(s)",                              // the evaluator
    (pn, justCheck) => Autocompletion.literal(pn, "pixel(s)", imageUnits)); // the autocompleter
```
</div>
</div>

`Autocompletion.literal` creates an array of literal completions from a `ParsedNode` and a variable number of `String` arguments.

So where does `imageUnits` come from? We need to catch it at runtime, so we register a `ParseStartListener` on the `parser`, and read the image calibration units at the time parsing is started:
<div class="tab-header"><span>Java</span><span class="selected">Python</span><span>JavaScript</span></div>
<div class="tab-body">
<div class="tab-content selected">
```java
StringBuilder imageUnits = new StringBuilder();
parser.addParseStartListener(() -> {
    imageUnits.setLength(0);
    imageUnits.append(image.getCalibration().getUnits());
});
```
</div>
<div class="tab-content">
```python
imageUnits = ""

def parsingStarted():
    global imageUnits
    imageUnits = preprocessing.getUnits()

parser.addParseStartListener(listener=ParseStartListener(parsingStarted))
```
</div>
<div class="tab-content">
```javascript
let imageUnits = "";
parser.addParseStartListener(() => {
  imageUnits = preprocessing.getUnits();
});
```
</div>
</div>
Find the full code here: [`Java`](https://github.com/nlScript/nlScript-tutorial-java/blob/main/src/main/java/nlScript/tutorial/preprocessing/Tutorial06.java) [`Python`](https://github.com/nlScript/nlScript-tutorial-python/blob/main/tutorial06.py) [`JavaScript`](https://github.com/nlScript/nlScript-tutorial-js/blob/main/docs/tutorial06.html).

Once autocompletion hits the `units` phrase, it displays a dropdown menu with the 2 options `pixel(s)` and `mm`, as desired.



## Prohibit further autocompletion: `Autocompleter.VETO`

However, when we start typing a value for `units`, e.g. we manually enter `cm`, it still shows `pixel(s)`, `mm` and additionally `.`. That's because our `Autocompleter` always returns the two options. We could decide to just skip autocompletion once the user starts to type. We can do this in the `Autocompleter`, by checking if the user already entered something, and in case s/he did, we return `Autocompletion.veto()`:
<div class="tab-header"><span>Java</span><span class="selected">Python</span><span>JavaScript</span></div>
<div class="tab-body">
<div class="tab-content selected">
```java
parser.defineType(
    "units",
    "{unitstring:[a-zA-Z()]:+}",
    pn -> !pn.getParsedString().equals("pixel(s)"),
    (pn, justCheck) -> pn.getParsedString().isEmpty()
                        ? Autocompletion.literal(pn, "pixel(s)", imageUnits)
                        : Autocompletion.veto(pn));
```
</div>
<div class="tab-content">
```python
def getAutocompletion(pn, justCheck):
    if len(pn.getParsedString()) == 0:
        return Autocompletion.literal(pn, ["pixel(s)", imageUnits])
    return Autocompletion.veto(pn)

parser.defineType(
    "units",
    "{unistring:[a-zA-Z()]:+}",
    lambda pn: pn.getParsedString() != "pixel(s)",
    getAutocompletion)
```
</div>
<div class="tab-content">
```javascript
parser.defineType(
    "units",
    "{unitstring:[a-zA-Z()]:+}",
    pn => pn.getParsedString() !== "pixel(s)",
    (pn, justCheck) => pn.getParsedString().length === 0
        ? nlScript.Autocompletion.literal(pn, ["pixel(s)", imageUnits])
        : nlScript.Autocompletion.veto(pn));
```
</div>
</div>
Find the full code here: [`Java`](https://github.com/nlScript/nlScript-tutorial-java/blob/main/src/main/java/nlScript/tutorial/preprocessing/Tutorial07.java) [`Python`](https://github.com/nlScript/nlScript-tutorial-python/blob/main/tutorial07.py) [`JavaScript`](https://github.com/nlScript/nlScript-tutorial-js/blob/main/docs/tutorial07.html).



## Dynamically re-defining types

The above solution works, but the meaning is a little different from what we wanted to achieve originally. Although it shows `pixel(s)` and `mm` as completion options, it accepts any string (we defined the pattern to parse to be `a-zA-Z()`). In fact, we'd ideally like to restrict possible types to `pixel(s)` and `mm`. And we can implement this by dynamically changing the parser, i.e. re-defining the type `units`. We do this again with a `ParseStartListener`:
<div class="tab-header"><span>Java</span><span class="selected">Python</span><span>JavaScript</span></div>
<div class="tab-body">
<div class="tab-content selected">
```java
parser.addParseStartListener(() -> {
    String unitsString = image.getCalibration().getUnits();

    parser.undefineType("units");

    // Re-define the 'units' type
    parser.defineType("units", "pixel(s)", pn -> false);
    parser.defineType("units", unitsString, pn -> true);
});
```
</div>
<div class="tab-content">
```python
def parsingStarted():
    unitsString = preprocessing.getUnits()

    parser.undefineType("units")

    # Re-define the 'units' type
    parser.defineType("units", "pixel(s)", lambda pn: False)
    parser.defineType("units", unitsString, lambda pn: True)


parser.addParseStartListener(listener=ParseStartListener(parsingStarted))
```
</div>
<div class="tab-content">
```javascript
parser.addParseStartListener(() => {
    imageUnits = preprocessing.getUnits();

    parser.undefineType("units");

    // Re-define the 'units' type
    parser.defineType("units", "pixel(s)", pn => false);
    parser.defineType("units", imageUnits, pn => true);
});

```
</div>
</div>
Find the full code here: [`Java`](https://github.com/nlScript/nlScript-tutorial-java/blob/main/src/main/java/nlScript/tutorial/preprocessing/Tutorial08.java) [`Python`](https://github.com/nlScript/nlScript-tutorial-python/blob/main/tutorial08.py) [`JavaScript`](https://github.com/nlScript/nlScript-tutorial-js/blob/main/docs/tutorial08.html).

This does now exactly what we wanted, i.e. it acts like the `units` type was defined from the beginning on with the calibration unit of the image.

This is indeed a very powerful feature, as it allows for dynamic re-definition of the parsed language, based on current runtime circumstances.


<br>
<br>
<br>

<script>

let selectedTab = 0;

tabHeaders = document.getElementsByClassName("tab-header");
for(tabHeader of tabHeaders) {
  let tabBody = tabHeader.nextElementSibling;
  console.log("tabBody");
  console.log(tabBody);
  for(let i = 0; i < tabHeader.children.length; i++) {
    tabHeader.children[i].onclick = clickTab;
    if(i == selectedTab) {
      console.log("Clicked tab " + i);
      tabHeader.children[i].classList.add('selected');
      tabBody.children[i].classList.add('selected');
    } else {
      console.log("Did not click tab " + i);
      tabHeader.children[i].classList.remove('selected');
      tabBody.children[i].classList.remove('selected');
    }
  }
}
</script>




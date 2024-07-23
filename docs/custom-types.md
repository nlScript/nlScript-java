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


## Custom types
### Defining a new generic color type based on RGB values
Custom types can be defined using built-in types and other custom types. In this way, an entire hierarchy of types can be built up. To demonstrate how custom types are defined, I will implement a type `my-color`, because color is perfectly suited to visualize the concept of types, although a type `color` already exists. In contrast to the existing `color` type, I want `my-color` to match `rgb(...)`, i.e. the string `rgb` followed by a 3-tuple with the values for red, green and blue.

Defining a custom type is similar to defining a sentence as shown earlier, indeed `defineSentence` is just a shortcut for defining (or extending) the type `sentence`:

<div class="tab-header"><span>Java</span><span class="selected">Python</span><span>JavaScript</span></div>
<div class="tab-body">
<div class="tab-content selected">
```java
Parser parser = new Parser();
parser.defineType("my-color", "rgb({red:int}, {green:int}, {blue:int})", pn -> {
	int red = (int) pn.evaluate("red");
	int green = (int) pn.evaluate("green");
	int blue = (int) pn.evaluate("blue");
	return new int[] {red, green, blue};
});
```
</div>
<div class="tab-content">
```python
parser = Parser()

def evaluateColor(pn):
    red = pn.evaluate("red")
    green = pn.evaluate("green")
    blue = pn.evaluate("blue")
    return (red, green, blue)

parser.defineType(
    "my-color",
    "rgb({red:int}, {green:int}, {blue:int})",
    evaluateColor)
```
</div>
<div class="tab-content">
```javascript
let parser = new nlScript.Parser();
parser.defineType(
    "my-color",
    "rgb({red:int}, {green:int}, {blue:int})",
    pn => {
        let red = pn.evaluate("red")
        let green = pn.evaluate("green")
        let blue = pn.evaluate("blue")
        return [red, green, blue];
    });
```
</div>
</div>


`defineType` has 3 arguments:
- A name for the type, which can be used in later type definitions<br>Like an integral number is of type `int`, our color will be of type `my-color`.

- A pattern that defines how the type should be parsed<br>`"rgb({red:int}, {green:int}, {blue:int})"` means that our new type will consist of the string 'rgb', followed by a 3-tuple with integral values named 'red', 'green' and 'blue' for the red, green and blue color component. `my-color` consists of three variables `red`, `green` and `blue`, which are of type `int` and which can be accessed in the evaluator by their name.

- An evaluator<br>The evaluator returns an object corresponding to the parsed and interpreted type, here an `int[]` (**Java**), a `tuple` (**Python**) or an `array` (**JavasScript**), with the red, green and blue color components as elements.

The new type can now be used in `defineSentence`, or to define other types via `defineType`:

```java
parser.defineSentence("Set the drawing color to {color:my-color}.", ...);
```


### Defining a new generic color type based on a set of named colors

We can also define a new type with a fixed set of options, i.e. a set of pre-defined named colors. This is implemented by repeatedly defining a type with different patterns:
<div class="tab-header"><span>Java</span><span class="selected">Python</span><span>JavaScript</span></div>
<div class="tab-body">
<div class="tab-content selected">
```java
Parser parser = new Parser();
parser.defineType("my-color", "white", pn -> new int[] { 255, 255, 255 });
parser.defineType("my-color", "black", pn -> new int[] {   0,   0, 0   });
parser.defineType("my-color", "red",   pn -> new int[] { 255,   0, 0   });
parser.defineType("my-color", "green", pn -> new int[] {   0, 255, 0   });
...
parser.defineSentence("Set the drawing color to {color:my-color}.", ...);
```
</div>
<div class="tab-content">
```python
parser = Parser()
parser.defineType("my-color", "white", lambda pn: (255, 255, 255))
parser.defineType("my-color", "black", lambda pn: (  0,   0,   0))
parser.defineType("my-color", "red",   lambda pn: (255,   0,   0))
parser.defineType("my-color", "green", lambda pn: (  0, 255,   0))
...
parser.defineSentence("Set the drawing color to {color:my-color}.", ...);
```
</div>
<div class="tab-content">
```javascript
let parser = new nlScript.Parser();
parser.defineType("my-color", "white", pn => [255, 255, 255]);
parser.defineType("my-color", "black", pn => [  0,   0,   0]);
parser.defineType("my-color", "red",   pn => [255,   0,   0]);
parser.defineType("my-color", "green", pn => [  0, 255,   0]);
...
parser.defineSentence("Set the drawing color to {color:my-color}.", ...);
```
</div>
</div>

### Combining generic and preset type definitions
The above two concepts can also be combined, which allows the user to select a color from a pre-defined list, but also leaves the freedom to provide a custom color which is not in the list, by specifying its RGB values:

<div class="tab-header"><span>Java</span><span class="selected">Python</span><span>JavaScript</span></div>
<div class="tab-body">
<div class="tab-content selected">
```java
Parser parser = new Parser();
parser.defineType("my-color", "rgb({red:int}, {green:int}, {blue:int})", pn -> {
	int red = (int) pn.evaluate("red");
	int green = (int) pn.evaluate("green");
	int blue = (int) pn.evaluate("blue");
	return new int[] {red, green, blue};
});
parser.defineType("my-color", "white", pn -> new int[] { 255, 255, 255 });
parser.defineType("my-color", "black", pn -> new int[] {   0,   0, 0   });
parser.defineType("my-color", "red",   pn -> new int[] { 255,   0, 0   });
parser.defineType("my-color", "green", pn -> new int[] {   0, 255, 0   });
...
parser.defineSentence("Set the drawing color to {color:my-color}.", ...);
```
</div>
<div class="tab-content">
```python
parser = Parser()

def evaluateColor(pn):
    red = pn.evaluate("red")
    green = pn.evaluate("green")
    blue = pn.evaluate("blue")
    return (red, green, blue)

parser.defineType(
    "my-color",
    "rgb({red:int}, {green:int}, {blue:int})",
    evaluateColor)

parser.defineType("my-color", "white", lambda pn: (255, 255, 255))
parser.defineType("my-color", "black", lambda pn: (  0,   0,   0))
parser.defineType("my-color", "red",   lambda pn: (255,   0,   0))
parser.defineType("my-color", "green", lambda pn: (  0, 255,   0))
...
parser.defineSentence("Set the drawing color to {color:my-color}.", ...);
```
</div>
<div class="tab-content">
```javascript
let parser = new nlScript.Parser();
parser.defineType(
    "my-color",
    "rgb({red:int}, {green:int}, {blue:int})",
    pn => {
        let red = pn.evaluate("red")
        let green = pn.evaluate("green")
        let blue = pn.evaluate("blue")
        return [red, green, blue];
    });
parser.defineType("my-color", "white", pn => [255, 255, 255]);
parser.defineType("my-color", "black", pn => [  0,   0,   0]);
parser.defineType("my-color", "red",   pn => [255,   0,   0]);
parser.defineType("my-color", "green", pn => [  0, 255,   0]);
...
parser.defineSentence("Set the drawing color to {color:my-color}.", ...);
```
</div>
</div>



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



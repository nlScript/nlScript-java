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
# Type hierarchy


A key feature is the definition of custom types, which itself can be composed of built-in or custom types in turn, to build a type hierarchy. Custom types are defined like sentences, but using the parser's `defineType()` method:
<div class="tab-header"><span>Java</span><span class="selected">Python</span><span>JavaScript</span></div>
<div class="tab-body">
<div class="tab-content selected">
```java
Parser parser = new Parser();
parser.defineType("interval", "[{from:int}; {to:int}]", pn -> null, true);
```
</div>
<div class="tab-content">
```python
parser = Parser()
parser.defineType("interval", "[{from:int}; {to:int}]", lambda pn: None, True);
```
</div>
<div class="tab-content">
```javascript
let parser = new nlScript.Parser();
parser.defineType("interval", "[{from:int}; {to:int}]", pn => undefined, true);
```
</div>
</div>




Its first argument is the name of the type ('interval'). Analogous to `defineSentence()`,  the second argument is a parametrized string (here, two integral numbers with names 'from' and 'to', separated by a semicolon and enclosed by square brackets). The third and fourth argument are skipped here for simplicity. The expression defined above parses, e.g., '[-3; 3]'.

Custom types are used to define other custom types or to define sentences, e.g.:
```java
parser.defineSentence("Set the stage limits to {limits:interval}.", ...);
```
Types are typically built up of other types, so that a type hierarchy arises. This hierarchy is preserved after parsing and is reflected in the tree structure of the `ParsedNode` (`pn`) object provided to the `Evaluator`. For debugging purposes, the parsed tree can be visualized:
```java
try {
	ParsedNode pn = parser.parse("Set the stage limits to [-3; 3].", null);
	System.out.println(GraphViz.toVizDotLink(pn));
} catch(ParseException e) {
	System.out.println(GraphViz.toVizDotLink(e.getRoot()));
	e.printStackTrace();
}
```

![](images/parsed-trees.png)

Here, the root node `S'` consists of the parsed `program` followed by `EOI` (end-of-input). A `program` consists of a number of `sentence`s (here, a single one is parsed). The parsed `sentence` consists of the string 'Set the stage limits to', followed by one or more whitespace characters, followed by a variable named 'limits', followed by a fullstop etc.

Nodes are color-coded for their parsing state: If a node was parsed successfully, it is outlined green, if a parsing error occurred, it is outlined red. Nodes which encountered the end of the input during parsing are outlined orange. The right image above shows the parsed tree resulting from parsing
```java
ParsedNode pn = parser.parse("Set the stage limits to [a; 3].", null);
```
<br><br>

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



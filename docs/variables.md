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


# Specifying variables
## General
Variables in sentences (and later also in custom types) are specified via `{name:type}`. In
```java
parser.defineSentence(
    "Apply Gaussian blurring with a standard deviation of {stddev:float} pixel(s).", ...);
```
there is one variable; a floating point number called `stddev`. 

Next to `float`, a number of built-in types exist (see below).

Variable names can consist of any character but `:`, `{` and `}`.

`type` must be a valid type, i.e. either a built-in type or a custom type. Type names must start with a letter or underscore and they must end with either a letter, a digit or an underscore. They may contains hyphens (`-`). (The regular expression for type names is `[A-Za-z_] ([A-Za-z0-9-_]* [A-Za-z0-9_])?`).

## Quantifiers
Variable specifications may contain a quantifier: **`{name:type:quantifier}`**. A quantifier specifies how often the `type` is matched. For example, `{pin:digit:4}` would match a 4-digit number called 'pin'. Valid quantifiers:
- `{pin:digit:4}` matches a number consisting of four digits.
- `{pin:digit:3-5}` matches a number consisting of 3 to 5 digits.
- `{pin:digit:*}` matches a number consisting of 0 to infinity digits.
- `{pin:digit:+}` matches a number consisting of 1 to infinity digits.
- `{pin:digit:?}` matches a number consisting of 0 to 1 digits.

If a quantifier is used, the variable evaluates to `java.lang.Object[]`, where the actual type of each array entry corresponds to `type` (here: `digit`). See also the paragraph "Evaluating the parsed text".

## Literals
Variables can also be specified without type and quantifier, like **`{literal}`**. In this case, it matches the constant string 'literal'.
<br>

In summary, these options exist:
| Specification            | Meaning                                                                     |
| ------------------------ | --------------------------------------------------------------------------- |
| `{name:type}`            | a variable of type `type` called `name`                                     |
| `{name:type:quantifier}` | a variable of type `type` called `name`, repeated according to `quantifier` |
| `{literal}`              | a variable of type `type` called `name`                                     |


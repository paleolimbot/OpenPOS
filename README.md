# BetterBark

Regular documentation is in preparation...

## BetterBark Custom Actions

So you've found yourself to the BetterBark app, among what seems like thousands of other barcode-enabled apps. You've probably come to the same conclusion that I have, which is that most of them were written by nerds and are super hard to use. Hopefully you'll find that the default behaviour of BetterBark is significantly better, but the real power of BetterBark is that **a significant portion of the interface is programmed in JSON**. That means that you can modify what happens when there is a **new barcode** (or key-in), when there is a **repeat barcode**, or when the user **clicks on a scanned item**. To give you an example, let's examine the JSON for the default **new barcode** action. As a reminder, you'll remember that by default, the phone vibrates, then opens a dialog that lets you choose to find the book in a library (if the result was a book), search Google, or search Amazon.

```json
{
  "type": "list",
  "actions": [
    {
      "type": "vibrate"
    },
    {
      "type": "chooser",
      "title": "{{_description}}",
      "negative_text": "Cancel",
      "labels": ["Find this in a Library ®", "Search Google", "Search Amazon", "Add to Session"],
      "actions": [
        {
          "type": "intent",
          "uri_format": "http://www.worldcatlibraries.org/registry/gateway?isbn={{isbn13}}"
        },
        {
          "type": "intent",
          "uri_format": "https://www.google.ca/search?q={{gtin13}}"
        },
        {
          "type": "intent",
          "uri_format": "https://www.amazon.com/s/field-keywords={{gtin13}}"
        },
        {
          "type": "session",
          "action": "add"
        }
      ]
    }
  ]
}
```

What we have here is a nested set of **action** objects, described in JSON (they are inflated into Java objects at runtime). We have a **list** (which executes actions sequentially), a **chooser**, which lets us choose to execute one of several actions, and an **intent** action, which launches an a URI. Some parameters have **parameters** surrounded by curly braces (`{{param}}`), which means they are automatically replaced by the corresponding attribute of the barcode. If the attribute doesn't exist, the action doesn't apply and in some cases is not shown (like in a **chooser**), and in others just not executed. Some parameters that are useful are `{{gtin13}}`, which is the 13-digit version of the UPC, EAN, JAN, or GTIN that label most products sold in a retail establishment, `{{upca}}`, which is the 12-digit number on most American retail products, and`{{isbn13}}` or `{{isbn10}}`, which are on barcodes identifying books. The entire action subsystem works by assigning these parameters and using the values of them to make dymanic user interface choices based on input.

## Example: make a list of books in a bookcase

You may be surprised to know that the motivation for writing this entire app was to make a list of the books in my girlfriend's bookshelf. Oh, the things we do for love. As you can clearly see, I got carried away. To go about this task efficiently, I would like to scan all the books that have a barcode, entering by hand the numbers that don't scan (or are new enough to have ISBN numbers, but old enough not to have barcodes), and finally entering the titles of books that have no numbers whatsoever. This is quite different than the usual use of the app, so I need a new set of **actions** to make this happen. In english what I'm looking for is the following:

* **Vibrate** to let me know I've scanned something
* If the barcode I scanned isn't an ISBN number, display an error message in a **toast**
* If the barcode is an ISBN number, try to **look up** the details from [Goodreads](http://www.goodreads.com/).
* If the details aren't available, open a **dialog** that lets me enter the title and author.
* Save the result (if no error occurred).

To do this, we're going to need a few types of actions: the **list**, the **vibrate**, the **switch**, a **toast**, another **list**, a **lookup**, and a **dialog**.

### The list action

We will start with the **list** action, which simply executes a list of other **actions**. Since the first the we really want to do is vibrate to indicate we've hit a barcode and *then* do everything else, the root **action** is commonly a **list**.

```json
{
  "type": "list",
  "actions": [
    ...
  ]
}
```

All **actions** get declared like this: a JSON object (curly braces) with a **type** option. The **list** action in turn takes an option called **actions**, which is a JSON array of **other action objects** (declared using curly brackets just like their parents).

### The vibrate action

The first action in the **list** is a simple **vibrate** action, which takes no parameters and simply does one thing: a short buzz of the vibrator.

```json
{
  "type": "list",
  "actions": [
    {
      "type": "vibrate"
    }
  ]
}
```

The **vibrate** action is quite simple, and in fact takes no options. 

### The switch action

Now things get more complicated with the **switch** action, to switch based on whether or not the number read is an ISBN number. We do this by testing whether or not the `{{isbn13}}` field matches the regular expression `^.*?$`, which is to say, anything.

```json
{
  "type": "list",
  "actions": [
    ...,
    {
      "type": "switch",
      "key": "isbn13",
      "is_regex": true,
      "values": ["^.*?$"],
      "actions": [{
        ... //executed if the isbn13 field exists
      }],
      "default": {
        ... //executed if the isbn13 field does not exist
      }
    }
  ]
}
```

Here, we declare an **action** of **type switch**, with options **key** (the key on which to match values), **is_regex** (to indicate that the values we're matching are [regular expressions](https://en.wikipedia.org/wiki/Regular_expression)), **values** (the values that define what we're looking to match), **actions** (the actions that match the values above), and **default** (the default action to perform if none of the **values** are matched). It's much like a Java `switch` statement and a little like an if/else statement but has some limitations (checking for the existence of fields is a little tricky).

### The toast action

Next, we'll cover what happens in the **default** case (there is no field called **isbn13** in the item). This might happen if, say, while scanning books, I fall into a delerious haze and scan a can of tomato soup thinking it's a book. BetterBark is smart enough to automatically add the **isbn13** whenever the input is a valid ISBN-10 or a valid ISBN-13 (it is also smart enough to recognize ISSN number, parse GS1 fields, and convert GTIN-14 numbers to GTIN-13 numbers, among other things), so if I scan tomato soup the **switch** action will execute the **default** action, which I would like to display a message telling me that I'm delerious, and that the number is not a valid ISBN number. I will do this using a **toast** action (after the android syntax), although it could just as easily be a **dialog** action.

```json
{
  "type": "list",
  "actions": [
    ...,
    {
      "type": "switch",
      ...,
      "default": {
        "type": "toast",
        "message": "Number '{{_barcode_text}}' is not a valid ISBN number"
      }
    }
  ]
}
```

Here you may notice we use a "special" field, `{{_barcode_text}}`. Some other examples are `{{_barcode_type}}` (the type of barcode that was read, like EAN or Code128), `{{_description}}` (the human-readable description), and `{{_subtext}}` (the grey text that appears below the description). You can assign or use these fields like any other.

### A fancy list action

The next thing we want to do is look up details of the book from the interwebs to avoid having to type in all the information ourselves. But there is a catch, which is that if the lookup fails, we want to enter the title ourselves. This brings us to a special modification of the **list** action, which is to add the **execute_until** option, which makes the **list** execute all of its sub-actions *until one of them succeeds*. The defenition of "succeed" is a little bit vague, but in the case of a lookup action is well-defined: if the lookup returns information, it should return true. This allows us to look things up from multiple sources and display a dialog at the end if all of the actions fail.

```json
{
  "type": "list",
  "actions": [
    ...,
    {
      "type": "switch",
      ...,
      "actions": [{
        "type": "list",
        "execute_until": "true",
        "actions" : [
          ... //lookup action(s) go here.
        ]
      }],
      ...
    }
  ]
}
```

### The lookup action

Probably the most useful action, the **lookup** connects to whatever address you choose to pull useful metadata about the barcode. Some examples include [UPCDatabase.org](http://www.upcdatabase.org), [UPCDatabase.com](http://www.upcdatabase.com), [ISBNDB](http://www.isbndb.com), [Google Books](http://books.google.com/), and [Goodreads](http://www.goodreads.com). We're going to use Goodreads for this particular example, but you could use all of them with our fancy **list** action to get the first human-readable description that shows up.

The structure of the **lookup** action is the same as the others, but it needs a bunch of options to figure out where to look and how to parse the result.

```json
{
  "type": "list",
  "actions": [
    ...,
    {
      "type": "switch",
      ...,
      "actions": [{
        "type": "list",
        ...,
        "actions" : [
          {
            "type": "lookup",
            "name": "goodreads",
            "uri_format": "https://www.goodreads.com/search/index.xml?key=APIKEY&q={{isbn13}}",
            "mime_type": "text/xml",
            "key_map": {
              "title": "{{search/results/work/best_book/title}}",
              "authors": "{{search/results/work/best_book/author/name}}",
            },
            "invalid_check": {
              "{{search/total-results}}": "0"
            }
          }
        ]
      }],
      ...
    }
  ]
}
```

There's a lot of pieces in here (the lookup action took what felt like two weeks to write), so I'll go through it by option. The **name** option is *option*al (ha!), but helps provide more useful information when errors occur. The **uri_format** is the most important piece, specifying the address from which to fetch data. You'll note that I removed my API Key but you can [get your own](https://www.goodreads.com/api/keys) for free. The **mime_type** refers to the type of data that you'd find if you visisted the web address provided by **uri_format**. In the case of Goodreads this is "text/xml", but could also be "application/json" or "text/xml-rpc" for another API. The next part is the **key_map**, which maps data from the response to item parameters. BetterBark includes templates to look up things from lots of sources, so you only have to worry about this and **invalid_check** if you're super keen on implementing a new API.

### The dialog action

The **list** action with **execute_until**=true ensures that if the lookup succeeds, execution will continue to the next dialog. This particular **dialog** action will collect data (and place it in the parameter specified by **out_key**), but a suitably created one could also just display a message.

```json
{
  "type": "list",
  "actions": [
    ...,
    {
      "type": "switch",
      ...,
      "actions": [{
        "type": "list",
        ...,
        "actions" : [
          {
            "type": "lookup",
            ...
          },
          {
            "type": "dialog",
            "title": "Enter Title",
            "input_type": "text",
            "out_key": "title"
          }
        ]
      }],
      ...
    }
  ]
}
```

### The session action

After all that, we want to add the result to our **session**, which is the current list of things being scanned. Session management is done by the user elsewhere but the **session** action makes sure whatever is being scanned ends up on that list. We're going to wrap this in another **switch** to make sure only items that had a **title** parameter got added. With that, the final JSON describing our workflow is as follows:

```json
{
  "type": "list",
  "actions": [
    {
      "type": "vibrate"
    },
    {
      "type": "switch",
      "key": "isbn13",
      "is_regex": true,
      "values": ["^.*?$"],
      "actions": [{
        "type": "list",
        "execute_until": "true",
        "actions" : [
          {
            "type": "lookup",
            "name": "goodreads",
            "uri_format": "https://www.goodreads.com/search/index.xml?key={{APIKEY}}&q={{isbn13}}",
            "mime_type": "text/xml",
            "key_map": {
              "title": "{{search/results/work/best_book/title}}",
              "authors": "{{search/results/work/best_book/author/name}}",
              "_description": "{{search/results/work/best_book/title}} ({{search/results/work/best_book/author/name}})",
              "goodreads_link": "https://www.goodreads.com/book/show/{{search/results/work/best_book/id}}"
            },
            "invalid_check": {
              "{{search/total-results}}": "0"
            }
          },
          {
            "type": "dialog",
            "title": "Enter Title",
            "input_type": "text",
            "out_key": "title"
          }
        ]
      }],
      "default": {
        "type": "toast",
        "message": "Number '{{_barcode_text}}' is not a valid ISBN number"
      }
    },
    {
      "type": "switch",
      "key": "title",
      "values": ["^.*?$"],
      "actions": [{
        "type": "session",
        "action": "add"
      }]
    }
  ]
}
```

## Action reference

In case you're ever wondering, here's all the types and all the options you can use to create actions. All actions are declared as follows:

```json
{
  "type": ..., //required
  "name": ..., //optional
  "enabled": ..., //default: "true"
  "quiet": ..., /default: "true"
  ...
}
```

**Options**

* **type**: The type of action (list, etc..)
* **name**: This is optional, but may help provide more meaningful error messages
* **enabled**: If **enabled** is set to "false", the action will not run.
* **quiet**: Different actions have different implementations of what **quiet** being "false" means, but in general actions are designed to fail quietly instead of stop all subsequent actions from executing on failure. Setting **quiet** to "false" may change this.

**Return values**

In general, the return value of actions is ignored, except in the case of **ifelse**, and **list** actions that have the **execute_until** option set. In particular, **lookup** actions tend to return false on failure, and **dialog** actions return false if cancelled.

**Formatted Strings***

The term **formatted string** will be used often, which refers to a string that is formatted using the key/value pairs of the item. For example, a url containing the text `{{gtin13}}` will be replaced with the gtin13 field of the item.

### list

**List** actions execute a **list** of actions in order, possibly stopping execution should an action return the value specified in **execute_until**.

```json
{
  "type": "list",
  "execute_until": ..., //optional, can be "true" or "false"
  "actions" [ ... ] //an array of actions (required)
}
```

### ifelse

**Ifelse** actions use the return value of one action (**test**) to choose between two other actions. Options **if_true** and **if_false** can be missing.

```json
{
  "type": "ifelse",
  "test": ..., //required, must be an action
  "if_true": ..., //an action
  "if_false": ... //another action
}
```

### switch

**Switch** actions take item parameters from a single **key** and chooses an **action** based on the value. The **values** can be regular expressions if **is_regex** is set to true.

```json
{
  "type": "switch",
  "key": ..., //the key that contains the value to be compared
  "values": [ ... ], //an array containing values to match, in order
  "is_regex": ..., //default: false; specifies if the values are regular expressions
  "actions": [ ... ] //an array of actions corresponding to values above
  "default": ... //an action
}
```

### blank

The **blank** action takes no options, and does nothing. It is possibly useful

```json
{
  "type": "blank"
}
```

### session

The **session** action either adds or removes an item from a session.

```json
{
  "type": "session",
  "action": ..., //one of "add" or "remove"
}
```

### keyfilter

The **keyfilter** action removes (or keeps) keys whose names the patterns specified.

```json
{
  "type": "keyfilter",
  "action": ..., //One of "keep" or "remove"
  "keys": [ ... ], //an array of keys to keep (or remove)
  "is_regex": ..., //"true" if the values in "keys" are regular expressions
  "match_option": ..., //One of "contains" or "matches" (default: "matches")
}
```

### matches

The **matches** action matches values of parameters as opposed to keys.

```json
{
  "type": "matches",
  "key_map": {
    ...: ... //key/value pairs in the form "key": "expected_value"
  },
  "match_option": ..., //One of "contains" or "matches" (default: "matches")
  "is_regex": ..., //"true" if the values in "key_map" are regular expressions
  "invert_result": ..., //"true" if the result should be inverted (default: false)
  "logic": ... //one of "any" or "all" (default: "all")
}
```

### lookup

The **lookup** action downloads data from the uri specified in **uri_format**, parses it, and assigns the key/value pairs specified in **key_map**. This could just as easily be used to put information into a server-hosted database and getting a response code back.

```json
{
  "type": "lookup",
  "api_type": ..., //default: "REST", can also be "XML-RPC" or "JSON-RPC"
  "uri_format": ..., //a formatted string of the uri
  "request": ..., //request payload, for XML-RPC or JSON-RPC
  "header": {
    ...
  },
  "oauth1": {
    "api_key": ...,
    "secret": ...
  },
  "mime_type": ..., //return data type, one of "application/json", "text/xml", or "text/xml-rpc"
  "key_map": {
    ... // see below
  },
  "invalid_check": {
    ... // see below
  },
  "valid_check" {
    ... // se below
  }
}
```

**Options**

* **api_type**: Can be "REST", "XML-RPC", or "JSON-RPC", according to whatever the specification of your particular API is like.
* **uri_format**: This is a formatted string, that can use any information that has been addded to the item so far. The most useful of these are `{{gtin13}}`, which is the 13-digit version of the UPC, EAN, JAN, or GTIN that label most products sold in a retail establishment, `{{upca}}`, which is the 12-digit number on most American retail products, and`{{isbn13}}` or `{{isbn10}}`, which are on barcodes identifying books. For the raw code that was read, you can use `{{_barcode_text}}`, and for the type of barcode read, you can use `{{_barcode_type}}`.
* **request**: This is necessary for XML-RPC or JSON-RPC APIs, and is a formatted string containing the payload to be sent by the POST method.
* **header**: Some APIs require the api key to be sent as a header field, so this lets you pass arbitrary key/value pairs (the values are formatted strings) to the HTTP header.
* **oauth1**: Some APIs require Oauth authentication, which requires an API key and a secret. This implementation can only deal with very simple Oauth requests.
* **mime_type**: This indicates the return type of the data, one of "application/json", "text/xml", or "text/xml-rpc". This allows the app to choose the appropriate parser for the data.
* **key_map**: The key map is where you map data from the request to item parameters. The **key_map** JSON object has the format `"item_key": "{{path/to/data}}"`, where `{{path/to/data}}` represents where the data is in the JSON/XML/XML-RPC that is returned. A simple example is a [UPCDatabase.org](http://www.upcdatabase.org/) response (below), where we might want to assign the item parameter "name" using "{{itemname}}" with the **key_map** parameter `"name": "{{itemname}}"`. Many responses contain nested objects and arrays accessed using the `/` and `[]` syntax like this: `{{results[0]/author_info/name}}`. XML, JSON, and XML-RPC implementations are all pretty much identical, and if the path can't be found then the value is ignored. Note that the XML implementation does not include the root tag in the path.

```json
{
  "valid":"true",
  "number":"0111222333446",
  "itemname":"UPC Database Testing Code",
  "alias":"Testing Code",
  "description":"http:\/\/upcdatabase.org\/code\/0111222333446",
  "avg_price":"123.45",
  "rate_up":"12",
  "rate_down":"2"
}
```

* **invalid_check**: This provides a means by which to determine whether the lookup succeeded by testing key/value pairs that indicate failure. The above example uses the **invalid_check** `{"{{valid}}": "false"}`. It is possible to test for a missing value by setting the two equal to eachother (e.g. `{"{{itemname}}": "{{itemname}}"}`, because if the path `itemname` doesn't exist in the output, it won't be formatted.
* **valid_check**: Similar to above, this provides key/value pairs that must all match. The above example uses the **valid_check** `{"{{valid}}": "true"}`.
* **use_cache**: By default the app will cache responses so that databses aren't overwhelmed with scans of the same item. It may be that this is not desired (e.g. if a lookup call is being used to add an item to a server database).

### stringformat

The **stringformat** action simply generates a formatted string and assigns it to keys (ignoring the action if the string doesn't contain the any of the keys specified).

```json
{
  "type": "stringformat",
  "key_map": {
    ... // key/value pairs the format "newkey": "{{oldkey1}} doopy doo {{oldkey2}}
  }
}
```

### dialog

A **dialog** action displays a dialog, possibly saving the result if the **out_key** option is specified. You can do one of three things: display a message, have the user pick an option (and assign the value to a specified key), or collect information from a text box. The three usage cases are presented separately.

**Display a message**

```json
{
  "type": "dialog",
  "title": ..., // a formatted string
  "message": ..., // a formatted string
  "positive_text": ..., // a formatted string
  "neutral_text": ..., // a formatted string
  "negative_text": ..., // a formatted string
  "out_key" ... // the key to which whatever value obtained should be stored
}
```

The value obtained from this dialog will be `_POSITIVE`, `_NEGATIVE`, `_NEUTRAL`, or `_CANCELLED`, if **outkey** is specified. The action will return false if the negative button is pressed or the dialog is cancelled.

**Select from items**

```json
{
  "type": "dialog",
  "title": ..., // a formatted string
  "positive_text": ..., // a formatted string
  "negative_text": ..., // a formatted string
  "input_hint": ..., // a formatted string
  "labels": [ ... ], // an array of formatted strings
  "values": [ ... ], // an array of formatted strings
  "out_key" ... // the key to which whatever value obtained should be stored
}
```

The value obtained from this dialog will be the item in **values** that corresponds to whatever the user clicked on (**labels** is what will be displayed). You can supply either **values** or **labels** or both, but **out_key** is required. Choices that contain formatted strings with unmapped keys will not be displayed. The action will return false if the negative button is pressed or the dialog is cancelled.

**Collect text data**

```json
{
  "type": "dialog",
  "title": ..., // a formatted string
  "positive_text": ..., // a formatted string
  "negative_text": ..., // a formatted string
  "input_type": ..., // one of "number" or "text" (required)
  "input_hint": ..., // a formatted string
  "out_key" ... // the key to which whatever value obtained should be stored
}
```

The value obtained from this dialog will be whatever the user types, or if the user types nothing (or cancells the dialog or pressed the negative button), no value will be set for the **out_key** specified. The option **out_key** is required.

### chooser

This action is kind of like a **dialog** and a **switch** combined, and lets the user pick between actions. Labels that contain mapped keys that do not exist are not shown, and actions that are not applicable are not shown.

```json
{
  "type": "chooser",
  "title": ..., // a formatted string
  "negative_text": ...,
  "labels": [ ... ], // an array of formatted strings
  "actions": [ ... ] // an array of actions
}
```

### details

This action displays the details dialog, assigning the output value the same as for the **dialog** action (if **out_key** is specified).

```json
{
  "type": "details",
  "title": ..., // optional, a formatted string
  "positive_text": ..., // optional, a formatted string
  "negative_text": ..., // optional, a formatted string
}
```

### intent

This action launches or broadcasts information using an [Android Intent](https://developer.android.com/guide/components/intents-filters.html). Most commonly, use this to launch a URL in a browser.

```json
{
  "type": "intent",
  "uri_format": ..., //a formatted string of the uri to launch
  "action": ..., // defaults to ACTION_VIEW
  "intent_type": ..., //defaults to "activity", could also be "broadcast"
  "extras": {
    ... // key/value pairs to put in the intent extras
  },
  "format_extras": ... //set to "true" if extra values are formatted strings.
}
```

### vibrate

This action launches the vibrator for the specified duration (default 150 ms).

```json
{
  "type": "vibrate",
  "duration": ... //the number of milliseconds to vibrate (default 150)
}
```

### toast

This action launches an Android Toast message to the user.

```json
{
  "type": "toast",
  "message": ..., //a formatted string
  "duration": ... //milliseconds duration (default: LENGTH_SHORT)
}
```

## Action Templates

### Lookup: UPCDatabase.org

Get a [UPCDatabase.org](http://www.upcdatabase.org) API Key [here](http://upcdatabase.org/signup).

```json
{
  "type": "lookup",
  "name": "upcdb",
  "uri_format": "http://api.upcdatabase.org/json/{{APIKEY}}/{{gtin13}}",
  "mime_type": "application/json",
  "api_type": "REST",
  "key_map": {
    "upcdb_error": "{{reason}}",
    "itemname": "{{itemname}}",
    "alias": "{{alias}}",
    "upcdb_link": "http://m.upcdatabase.org/code/{{number}}",
    "_description": "{{description}}"
  },
  "valid_check": {
    "{{valid}}": "true"
  }
}
```

### Lookup: ISBNDB.com

Get a [ISBNDB](http://www.isbndb.com) API Key [here](http://isbndb.com/account/logincreate).

```json
{
  "type": "lookup",
  "name": "isbndb",
  "encoding": "windows-1251",
  "mime_type": "application/json",
  "api_type": "REST",
  "uri_format": "http://isbndb.com/api/v2/json/{{APIKEY}}/book/{{isbn13}}",
  "key_map": {
    "title": "{{data[0]/title}}",
    "authors": "{{data[0]/author_data[; ]/name}}",
    "_description": "{{data[0]/title}} ({{data[0]/author_data[; ]/name}})",
    "dewey_normal": "{{data[0]/dewey_normal}}",
    "lcc_number": "{{data[0]/lcc_number}}",
    "publisher": "{{data[0]/publisher_text}}",
    "isbndb_error": "{{error}}",
    "isbndb_link": "http://isbndb.com/book/{{data[0]/book_id}}"
  },
  "valid_check": {
    "{{error}}": "{{error}}"
  }
}
```

### Lookup: Semantics3

Get a [Semantics3](http://www.semantics3.com) API Key [here](https://dashboard.semantics3.com/signup).

```json
{
  "type": "lookup",
  "name": "sem3",
  "attribution": "http://www.semantics3.com/",
  "api_type": "REST",
  "uri_format": "https://api.semantics3.com/v1/products?q=%7B%22ean%22:%22{{gtin13}}%22%7D",
  "mime_type": "application/json",
  "oauth1": {
    "api_key": "{{APIKEY}}",
    "secret": "{{SECRET}}"
  },
  "key_map": {
    "_description": "{{results[0]/name}}",
    "description": "{{results[0]/description}}",
    "price": "{{results[0]/price}}",
    "manufacturer": "{{results[0]/manufacturer}}",
    "brand": "{{results[0]/brand}}",
    "model": "{{results[0]/model}}",
    "sem3_error": "{{message}}"
  },
  "invalid_check": {
    "{{results_count}}": "0"
  }
}
```

### Lookup: UPCDatabase.com

Get a [UPCDatabase.com](http://www.upcdatabase.com) API Key [here](https://www.upcdatabase.com/join.asp).

```json
{
  "type": "lookup",
  "name": "upcdb",
  "uri_format": "https://www.upcdatabase.com/xmlrpc",
  "request": "<?xml version=\"1.0\"?><methodCall><methodName>lookup</methodName><params><param><value><struct><member><name>rpc_key</name><value>{{APIKEY}}</value></member><member><name>ean</name><value>{{gtin13}}</value></member></struct></value></param></params></methodCall>",
  "mime_type": "text/xml-rpc",
  "api_type": "XML-RPC",
  "key_map": {
    "_description": "{{description}}",
    "size": "{{size}}",
    "upcdb_status": "{{message}}",
    "upcdb_link": "https://www.upcdatabase.com/item/{{ean}}"
  },
  "valid_check": {
    "{{found}}": "true"
  }
}
```

### Lookup: SimpleUPC.com

Get a [SimpleUPC](http://www.simpleupc.com) API Key [here](http://www.simpleupc.com/price.php).

```json
{
  "type": "lookup",
  "name": "supc",
  "attribution": "http://www.simpleupc.com/",
  "api_type": "JSON-RPC",
  "mime_type": "application/json",
  "uri_format": "http://api.simpleupc.com/v1.php",
  "request": {
    "auth": "{{APIKEY}}",
    "method": "FetchProductByUPC",
    "params": {
      "upc": "{{upca}}"
    },
    "returnFormat": "json"
  },
  "key_map": {
    "simpleupc_error": "{{error/message}}",
    "_description": "{{result/description}}",
    "manufacturer": "{{result/manufacturer}}",
    "brand": "{{result/brand}}"
  },
  "valid_check": {
    "{{error/message}}": "{{error/message}}"
  }
}
```

### Lookup: Google Books

Get a [Google Books](http://books.google.com/) API Key [here](https://developers.google.com/books/docs/v1/using#APIKey).

```json
{
  "type": "lookup",
  "name": "GoogleBooks",
  "attribution": "http://books.google.com/",
  "api_type": "REST",
  "uri_format": "https://www.googleapis.com/books/v1/volumes?q=isbn:{{isbn13}}&key={{APIKEY}}",
  "mime_type": "application/json",
  "key_map": {
    "title": "{{items[0]/volumeInfo/title}}",
    "authors": "{{items[0]/volumeInfo/authors[; ]}}",
    "_description": "{{items[0]/volumeInfo/title}} ({{items[0]/volumeInfo/authors[; ]}})",
    "publisher": "{{items[0]/volumeInfo/publisher}}",
    "google_link": "{{items[0]/volumeInfo/infoLink}}"
  },
  "invalid_check": {
    "{{totalItems}}": "0"
  }
}
```

### Lookup: Goodreads

Get a [Goodreads](https://www.goodreads.com/) API Key [here](https://www.goodreads.com/api/keys).

```json
{
  "type": "lookup",
  "name": "goodreads",
  "attribution": "http://www.goodreads.com/",
  "api_type": "REST",
  "uri_format": "https://www.goodreads.com/search/index.xml?key={{APIKEY}}&q={{isbn13}}",
  "mime_type": "text/xml",
  "key_map": {
    "title": "{{search/results/work/best_book/title}}",
    "authors": "{{search/results/work/best_book/author/name}}",
    "_description": "{{search/results/work/best_book/title}} ({{search/results/work/best_book/author/name}})",
    "goodreads_link": "https://www.goodreads.com/book/show/{{search/results/work/best_book/id}}"
  },
  "invalid_check": {
    "{{search/total-results}}": "0"
  }
}
```


# ViewClone Module

## Description

Das ViewClone Module ermöglicht das effiziente Klonen von Titanium UI Views mit rekursiver Kopie aller Kind-Views, Properties und EventListener. Das Klonen erfolgt nativ auf Android-Ebene für maximale Performance.

## Features

- **Rekursive Klonung**: Kopiert komplexe View-Hierarchien vollständig
- **Property-Kopie**: Alle Titanium Properties werden übernommen
- **EventListener-Kopie**: Event-Listener werden in den Klon übertragen
- **Native Performance**: Klonen erfolgt auf Java-Ebene für schnelle Ausführung
- **Kompatibilität**: Unterstützt alle Ti.UI.View Typen (View, Label, Button, ImageView, etc.)
- **Template-Erstellung**: Erzeugt ListView-Templates aus bestehenden Views

## Accessing the ViewClone Module

Um das Modul in JavaScript zu verwenden:

```javascript
import viewclone from 'de.marcbender.viewclone';
// oder
var viewclone = require('de.marcbender.viewclone');
```

## Reference

### viewclone.cloneView(view)

Klonen einer TiViewProxy Instanz mit allen Properties und Kind-Views.

#### Arguments

| Name | Type | Description |
|------|------|-------------|
| view | TiViewProxy | Das zu klonende ViewProxy Objekt |

#### Returns

TiViewProxy - Das geklonte ViewProxy Objekt oder `null` bei einem Fehler.

#### Example

```javascript
const originalView = Ti.UI.createView({
    backgroundColor: 'red',
    width: 100,
    height: 100
});

const clonedView = viewclone.cloneView(originalView);
clonedView.backgroundColor = 'blue';
win.add(clonedView);
```

#### Complex View Example

```javascript
// Erstelle ein komplexes Layout mit Kind-Views
const container = Ti.UI.createView({
    layout: 'vertical',
    backgroundColor: '#eee',
    width: 300,
    height: 200
});

const header = Ti.UI.createLabel({
    text: 'Header',
    backgroundColor: '#0066cc',
    color: '#fff',
    width: Ti.UI.FILL,
    height: 40
});

const content = Ti.UI.createLabel({
    text: 'Content',
    color: '#333',
    width: Ti.UI.FILL,
    height: Ti.UI.FILL
});

container.add(header);
container.add(content);
win.add(container);

// Klonen des kompletten Layouts
const clonedContainer = viewclone.cloneView(container);
clonedContainer.top = 250;
win.add(clonedContainer);
```

## Usage

### Einfaches Klonen

```javascript
import viewclone from 'de.marcbender.viewclone';

const original = Ti.UI.createLabel({
    text: 'Hello World',
    color: '#000',
    font: { fontSize: 16 }
});

const clone = viewclone.cloneView(original);
clone.text = 'Cloned Text';
```

### Rekursive Klonung mit Kind-Views

Das Modul klonet automatisch alle Kind-Views rekursiv:

```javascript
const parent = Ti.UI.createView({
    layout: 'vertical'
});

const child1 = Ti.UI.createLabel({ text: 'Child 1' });
const child2 = Ti.UI.createLabel({ text: 'Child 2' });

parent.add(child1);
parent.add(child2);

// Klonen kloniert auch child1 und child2
const clonedParent = viewclone.cloneView(parent);
```

### EventListener Handling

EventListener werden automatisch in den Klon kopiert. Zusätzliche EventListener können nach dem Klonen hinzugefügt werden:

```javascript
const button = Ti.UI.createButton({ title: 'Click me' });

button.addEventListener('click', function(e) {
    console.log('Original clicked');
});

const clonedButton = viewclone.cloneView(button);

// Füge eigenen Listener zum Klon hinzu
clonedButton.addEventListener('click', function(e) {
    console.log('Cloned clicked');
});
```

### viewclone.createTemplateFromView(view)

Erzeugt ein ListView Template aus einer TiUIView. Konvertiert eine komplexe View-Hierarchie in ein ListView-Template mit `childTemplates`.

#### Arguments

| Name | Type | Description |
|------|------|-------------|
| view | TiViewProxy | Das Source-View für das Template |

#### Returns

KrollDict - Ein Dictionary mit dem Template-Definition oder `null` bei einem Fehler.

#### Example

```javascript
// Erstelle eine komplexe View
const sourceView = Ti.UI.createView({
    backgroundColor: '#fff',
    width: 300,
    height: 100,
    layout: 'horizontal'
});

const imageView = Ti.UI.createImageView({
    image: '/images/icon.png',
    width: 50,
    height: 50,
    left: 0
});

const label = Ti.UI.createLabel({
    text: 'Item Title',
    color: '#000',
    font: { fontSize: 18, fontWeight: 'bold' },
    left: 60,
    top: 0
});

const subLabel = Ti.UI.createLabel({
    text: 'Subtitle text',
    color: '#888',
    font: { fontSize: 14 },
    left: 60,
    top: 25
});

sourceView.add(imageView);
sourceView.add(label);
sourceView.add(subLabel);

// Erzeuge das Template
const myTemplate = viewclone.createTemplateFromView(sourceView);

// Verwende das Template in einer ListView
const listView = Ti.UI.createListView({
    templates: {
        'item': myTemplate
    },
    defaultItemTemplate: 'item'
});

// Items mit Daten füllen
const items = [
    { pic: '/images/item1.png', info: 'Item 1', es_info: 'Details 1' },
    { pic: '/images/item2.png', info: 'Item 2', es_info: 'Details 2' }
];

// Erstelle Sections und füge Items hinzu
const section = Ti.UI.createListSection({});
section.setItems(items);
listView.appendSection(section);
```

#### Generated Template Structure

Das erzeugte Template hat folgende Struktur:

```javascript
{
    tiProxy: {
        className: 'org.appcelerator.titanium.proxy.TiViewProxy'
    },
    properties: {
        backgroundColor: '#fff',
        width: 300,
        height: 100,
        layout: 'horizontal'
    },
    childTemplates: [
        {
            tiProxy: {
                className: 'org.appcelerator.titanium.proxy.TiImageViewProxy'
            },
            bindId: 'pic',
            properties: {
                width: 50,
                height: 50,
                left: 0
            }
        },
        {
            tiProxy: {
                className: 'org.appcelerator.titanium.proxy.TiLabelProxy'
            },
            bindId: 'info',
            properties: {
                color: '#000',
                font: { fontSize: 18, fontWeight: 'bold' },
                left: 60,
                top: 0
            }
        },
        {
            tiProxy: {
                className: 'org.appcelerator.titanium.proxy.TiLabelProxy'
            },
            bindId: 'es_info',
            properties: {
                color: '#888',
                font: { fontSize: 14 },
                left: 60,
                top: 25
            }
        }
    ]
}
```

## Author

**Marc Bender**

## License

Apache Public License

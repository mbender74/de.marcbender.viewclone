# ViewClone - Titanium Android Module

Recursive deep cloning of `Ti.UI.View` objects with all properties and child views.

## Overview

The Titanium SDK's built-in `TiViewProxy.clone()` shares the same V8/JavaScript wrapper (KrollObject) between the original and the clone, which prevents the clone from having its own independent JavaScript identity. ViewClone solves this by creating a truly independent clone — a new proxy instance of the same type, with all properties copied and all child views cloned recursively.

## Installation

### Option 1: Local module

1. Build the module:

   ```bash
   cd android && ti build -p android --build-only
   ```

2. Copy the distribution zip into your app's `modules` directory:

   ```bash
   unzip -o android/dist/de.marcbender.viewclone-android-1.0.0.zip -d /path/to/your/app/modules_temp
   mv modules_temp/modules/android/de.marcbender.viewclone /path/to/your/app/modules/android/
   rm -rf modules_temp
   ```

### Option 2: Global installation

Copy the distribution zip into your Titanium modules directory:

- **Linux**: `~/.titanium/modules/android/de.marcbender.viewclone/1.0.0/`
- **macOS**: `~/Library/Application Support/Titanium/modules/android/de.marcbender.viewclone/1.0.0/`
- **Windows**: `C:\ProgramData\Titanium\modules\android\de.marcbender.viewclone\1.0.0\`

### Register the module

Add the module to your `tiapp.xml`:

```xml
<modules>
  <module platform="android" version="1.0.0">de.marcbender.viewclone</module>
</modules>
```

## API

### `cloneView(proxy)`

Creates a deep clone of a `Ti.UI.View` proxy, including all properties and nested child views.

**Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| `proxy` | `TiViewProxy` | The view proxy to clone |

**Returns:** `TiViewProxy | null` — A new independent view proxy, or `null` if cloning fails.

**What gets cloned:**

- All properties (positioning, styling, layout, fonts, colors, etc.)
- Child views — recursively cloned and added to the cloned parent
- The view type (`apiName`) — the clone is the same Ti.UI type as the original

**What does NOT get cloned:**

- Event listeners — add new listeners to the clone as needed

## Example Usage

```js
const viewclone = require('de.marcbender.viewclone');

// --- Clone a simple label ---
const originalLabel = Ti.UI.createLabel({
  text: 'Hello',
  color: '#000',
  font: { fontSize: 18, fontWeight: 'bold' },
  top: 10,
  left: 10,
  width: 200,
  height: 40,
  backgroundColor: '#f00'
});

const clonedLabel = viewclone.cloneView(originalLabel);
clonedLabel.text = 'Hello (clone)';
clonedLabel.backgroundColor = '#0f0';
win.add(clonedLabel);

// --- Clone a complex layout with children ---
const container = Ti.UI.createView({
  top: 50,
  width: 300,
  height: 200,
  backgroundColor: '#ddd',
  layout: 'vertical',
  borderRadius: 10,
  borderWidth: 2,
  borderColor: '#333'
});

const header = Ti.UI.createLabel({
  text: 'Header',
  color: '#fff',
  backgroundColor: '#0066cc',
  width: Ti.UI.FILL,
  height: 40
});

const content = Ti.UI.createView({
  width: Ti.UI.FILL,
  height: 100,
  layout: 'horizontal'
});

content.add(Ti.UI.createLabel({ text: 'Left', color: '#333' }));
content.add(Ti.UI.createLabel({ text: 'Right', color: '#333' }));

container.add(header);
container.add(content);

const clonedContainer = viewclone.cloneView(container);
clonedContainer.backgroundColor = '#cfc';

// Access cloned children via the .children property
const children = clonedContainer.children;
if (children && children.length > 0) {
  children[0].text = 'Cloned Header';
}

win.add(clonedContainer);

// --- Clone a button and add a new event listener ---
const originalButton = Ti.UI.createButton({
  title: 'Click me',
  width: 200,
  height: 50
});

originalButton.addEventListener('click', function () {
  Ti.API.info('Original clicked');
});

const clonedButton = viewclone.cloneView(originalButton);
clonedButton.title = 'Cloned button';

// Event listeners are NOT cloned — add your own
clonedButton.addEventListener('click', function () {
  Ti.API.info('Clone clicked');
});

win.add(clonedButton);
```

## Important Notes

### Accessing children: use `.children`, not `.getChildren()`

On cloned views, access child views using the **`.children` property**, not the `.getChildren()` method:

```js
// Correct
const children = clonedView.children;

// Incorrect — this method is not available on cloned views
const children = clonedView.getChildren();
```

This is because `.children` is exposed via `@Kroll.getProperty` in the Titanium SDK, which maps it to a JavaScript property. The `getChildren()` method call form is not available on views created from the Java side.

### Native view properties

Properties like `rect`, `size`, and `visibleText` may return empty/zero values immediately after cloning because the native Android view is created lazily when the proxy is added to a visible window. The actual values will be populated once the view is rendered.

## Requirements

- Titanium SDK 13.2.0.GA or later
- Android platform

## License

Apache Public License — see [LICENSE](LICENSE) for details.

## Author

Marc Bender
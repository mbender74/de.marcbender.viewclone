// Test harness for ViewClone Module
// Demonstrates recursive deep cloning of complex Ti.UI.Views

// open a single window
const win = Ti.UI.createWindow({
	backgroundColor: '#fff',
	layout: 'vertical'
});


const scrollView = Ti.UI.createScrollView({
	showVerticalScrollIndicator: true,
	showHorizontalScrollIndicator: true,
	layout: 'vertical',
	contentHeight:'auto',
	showVerticalScrollIndicator:true,
	height: Ti.UI.FILL,
	width: Ti.UI.FILL
});

// Label for result display
const label = Ti.UI.createLabel({
	top: 60,
	left: 20,
	right: 20,
	font: { fontSize: 16, fontWeight: 'bold' },
	textAlign: Ti.UI.TEXT_ALIGNMENT_LEFT
});
scrollView.add(label);

// Import ViewClone Module
const viewclone = require('de.marcbender.viewclone');
Ti.API.info("module loaded");

label.text = "ViewClone Module loaded\n";

// Test 1: Clone a simple label
function testSimpleLabel() {
	Ti.API.info("Test 1: Clone a simple label");

	const originalLabel = Ti.UI.createLabel({
		text: 'Original Label',
		color: '#000',
		font: { fontSize: 18, fontWeight: 'bold' },
		top: 10,
		left: 10,
		width: 200,
		height: 40,
		backgroundColor: '#f00'
	});

	scrollView.add(originalLabel);

	const clonedLabel = viewclone.cloneView(originalLabel);
	if (clonedLabel) {
		Ti.API.info("Cloned label text: " + clonedLabel.text);
		Ti.API.info("Cloned label color: " + clonedLabel.color);
		clonedLabel.text = 'Cloned Label';
		clonedLabel.backgroundColor = '#0f0';
		clonedLabel.top = 20;
		scrollView.add(clonedLabel);
		Ti.API.info("Simple label cloned successfully");
		label.text += "Test 1: Simple Label - OK\n";
	} else {
		Ti.API.error("Failed to clone label");
		label.text += "Test 1: Simple Label - ERROR\n";
	}
}

// Test 2: Clone a complex layout with child views
function testComplexLayout() {
	Ti.API.info("Test 2: Clone a complex layout with child views");

	const originalContainer = Ti.UI.createView({
		top: 20,
		left: 20,
		width: 300,
		height: 200,
		backgroundColor: '#ddd',
		layout: 'vertical',
		borderRadius: 10,
		borderWidth: 2,
		borderColor: '#333'
	});

	// Add child views
	const header = Ti.UI.createLabel({
		text: 'Header',
		color: '#fff',
		font: { fontSize: 16, fontWeight: 'bold' },
		backgroundColor: '#0066cc',
		width: Ti.UI.FILL,
		height: 40
	});

	const content = Ti.UI.createView({
		width: Ti.UI.FILL,
		height: 100,
		backgroundColor: '#eee',
		layout: 'horizontal'
	});

	const text1 = Ti.UI.createLabel({
		text: 'Text 1',
		color: '#333',
		width: Ti.UI.SIZE,
		height: Ti.UI.SIZE
	});

	const text2 = Ti.UI.createLabel({
		text: 'Text 2',
		color: '#333',
		width: Ti.UI.SIZE,
		height: Ti.UI.SIZE
	});

	content.add(text1);
	content.add(text2);

	originalContainer.add(header);
	originalContainer.add(content);

	scrollView.add(originalContainer);

	// Clone the complex layout
	const clonedContainer = viewclone.cloneView(originalContainer);
	if (clonedContainer) {
		clonedContainer.top = 20;
		clonedContainer.left = 20;
		clonedContainer.backgroundColor = '#cfc';
		Ti.API.info("Complex layout cloned successfully");

		// Check cloned children (use .children property, not .getChildren() method)
		const children = clonedContainer.children;
		Ti.API.info("Cloned container children count: " + (children ? children.length : 0));

		if (children && children.length > 0) {
			// Modify header
			if (children[0]) {
				Ti.API.info("First child apiName: " + children[0].apiName);
				Ti.API.info("First child text: " + children[0].text);
				children[0].text = 'Cloned Header';
			}

			// Check content children
			if (children[1] && children[1].children) {
				const labels = children[1].children;

				Ti.API.info("Content children count: " + children[1].children.length);

				Ti.API.info("First child apiName: " + labels[0].apiName);
				Ti.API.info("First child text: " + labels[0].text);
				labels[0].text = 'Cloned text 1';

				Ti.API.info("Second child apiName: " + labels[1].apiName);
				Ti.API.info("Second child text: " + labels[1].text);
				labels[1].text = 'Cloned text 2';

			}
		}

		scrollView.add(clonedContainer);
		label.text += "Test 2: Complex Layout - OK\n";
	} else {
		Ti.API.error("Failed to clone complex layout");
		label.text += "Test 2: Complex Layout - ERROR\n";
	}
}

// Test 3: Clone a view
function testView() {
	Ti.API.info("Test 3: Clone a view");

	const originalButton = Ti.UI.createButton({
		title: 'Original Button',
		top: 20,
		left: 20,
		width: 200,
		height: 50,
		backgroundColor: '#0066cc',
		color: '#fff'
	});

	let clickCount = 0;
	originalButton.addEventListener('click', function(e) {
		clickCount++;
		Ti.API.info("Original Button clicked: " + clickCount);


		const clonedButton = viewclone.cloneView(originalButton);
		if (clonedButton) {
			clonedButton.title = 'Cloned Button '+clickCount;
			clonedButton.backgroundColor = '#66cc00';
			clonedButton.top = 20;
			clonedButton.count = clickCount;

			// Add event listener to the clone
			clonedButton.addEventListener('click', function(e) {
				alert("Cloned Button "+e.source.count+" clicked");
			});

			scrollView.add(clonedButton);
			Ti.API.info("View with event listener cloned successfully");
			label.text += "Test 3: Clone View - OK\n";
		} else {
			Ti.API.error("Failed to clone view");
			label.text += "Test 3: Clone View - ERROR\n";
		}
	});

	scrollView.add(originalButton);
}

// Run all tests
testSimpleLabel();
testComplexLayout();
testView();

// Completion message
label.text += "\nAll tests executed!\n";


win.add(scrollView);

win.open();
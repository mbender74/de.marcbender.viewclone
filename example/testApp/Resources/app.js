// Test-Harness für ViewClone Module
// Zeigt rekursives Klonen von komplexen Ti.UI.Views

// open a single window
const win = Ti.UI.createWindow({
	backgroundColor: '#fff',
	layout: 'vertical'
});


const scrollView = Ti.UI.createScrollView({
	showVerticalScrollIndicator: true,
	showHorizontalScrollIndicator: true,
	layout: 'vertical',
	height: '100%',
	width: '100%'
});

// Label für Ergebnisanzeige
const label = Ti.UI.createLabel({
	top: 20,
	left: 20,
	right: 20,
	font: { fontSize: 16, fontWeight: 'bold' },
	textAlign: Ti.UI.TEXT_ALIGNMENT_LEFT
});
scrollView.add(label);

// Import ViewClone Module
const viewclone = require('de.marcbender.viewclone');
Ti.API.info("module loaded");

label.text = "ViewClone Module geladen\n";

// Test 1: Einfaches Label klonen
function testSimpleLabel() {
	Ti.API.info("Test 1: Einfaches Label klonen");

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
		clonedLabel.text = 'Kloned Label';
		clonedLabel.backgroundColor = '#0f0';
		clonedLabel.top = 20;
		scrollView.add(clonedLabel);
		Ti.API.info("Einfaches Label erfolgreich geklont");
		label.text += "Test 1: Einfaches Label - OK\n";
	} else {
		Ti.API.error("Fehler beim Klonen des Labels");
		label.text += "Test 1: Einfaches Label - FEHLER\n";
	}
}

// Test 2: Komplexes Layout mit Kind-Views
function testComplexLayout() {
	Ti.API.info("Test 2: Komplexes Layout mit Kind-Views klonen");

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

	// Kind-Views hinzufügen
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

	// Klonen des komplexen Layouts
	const clonedContainer = viewclone.cloneView(originalContainer);
	if (clonedContainer) {
		clonedContainer.top = 20;
		clonedContainer.left = 20;
		clonedContainer.backgroundColor = '#cfc';
		Ti.API.info("Komplexes Layout erfolgreich geklont");

		// Kinder des Clones prüfen (children Property, nicht getChildren() Methode)
		const children = clonedContainer.children;
		Ti.API.info("Cloned container children count: " + (children ? children.length : 0));

		if (children && children.length > 0) {
			// Header ändern
			if (children[0]) {
				Ti.API.info("First child apiName: " + children[0].apiName);
				Ti.API.info("First child text: " + children[0].text);
				children[0].text = 'Kloned Header';
			}

			// Content-Kinder prüfen
			if (children[1] && children[1].children) {
				const labels = children[1].children;

				Ti.API.info("Content children count: " + children[1].children.length);
				
				Ti.API.info("First child apiName: " + children[0].apiName);
				Ti.API.info("First child text: " + children[0].text);
				children[0].text = 'Kloned Header';

				Ti.API.info("First child apiName: " + labels[0].apiName);
				Ti.API.info("First child text: " + labels[0].text);
				labels[0].text = 'Kloned text 1';

				Ti.API.info("Second child apiName: " + labels[1].apiName);
				Ti.API.info("Second child text: " + labels[1].text);
				labels[1].text = 'Kloned text 2';

			}
		}

		scrollView.add(clonedContainer);
		label.text += "Test 2: Komplexes Layout - OK\n";
	} else {
		Ti.API.error("Fehler beim Klonen des komplexen Layouts");
		label.text += "Test 2: Komplexes Layout - FEHLER\n";
	}
}

// Test 3: View mit EventListener klonen
function testViewWithEvents() {
	Ti.API.info("Test 3: View mit EventListener klonen");

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
		Ti.API.info("Original Button geklickt: " + clickCount);


		const clonedButton = viewclone.cloneView(originalButton);
		if (clonedButton) {
			clonedButton.title = 'Kloned Button';
			clonedButton.backgroundColor = '#66cc00';
			clonedButton.top = 20;
	
			// EventListener für den Klon hinzufügen
			clonedButton.addEventListener('click', function(e) {
				alert("Kloned Button geklickt");
			});
	
			scrollView.add(clonedButton);
			Ti.API.info("View mit EventListener erfolgreich geklont");
			label.text += "Test 3: View mit Events - OK\n";
		} else {
			Ti.API.error("Fehler beim Klonen der View mit Events");
			label.text += "Test 3: View mit Events - FEHLER\n";
		}
	});

	scrollView.add(originalButton);
}

// Alle Tests ausführen
testSimpleLabel();
testComplexLayout();
testViewWithEvents();

// Abschlussnachricht
label.text += "\nAlle Tests ausgeführt!\n";


win.add(scrollView);


// // generate random number, used to make each row appear distinct for this example
// function randomInt(max){
//   return Math.floor(Math.random() * max) + 1;
// }

// var IMG_BASE = 'assets/images/';
// var defaultFontSize = Ti.Platform.name === 'android' ? 16 : 14;

// var tableData = [];

// for (var i=1; i<=120; i++){
//   var row = Ti.UI.createTableViewRow({
//     className: 'forumEvent', // used to improve table performance
//     backgroundSelectedColor: 'white',
//     rowIndex: i, // custom property, useful for determining the row during events
//     height: 110
//   });

//   var imageAvatar = Ti.UI.createImageView({
//     image: IMG_BASE + 'tab1.png',
//     left: 10, top: 5,
//     width: 50, height: 50
//   });
//   row.add(imageAvatar);

//   var labelUserName = Ti.UI.createLabel({
//     color: '#576996',
//     font: {fontFamily:'Arial', fontSize: defaultFontSize+6, fontWeight: 'bold'},
//     text: 'Fred Smith ' + i,
//     left: 70, top: 6,
//     width: 200, height: 30
//   });
//   row.add(labelUserName);

//   var labelDetails = Ti.UI.createLabel({
//     color: '#222',
//     font: {fontFamily:'Arial', fontSize: defaultFontSize+2, fontWeight: 'normal'},
//     text: 'Replied to post with id ' + randomInt(1000) + '.',
//     left: 70, top: 44,
//     width: 360
//   });
//   row.add(labelDetails);

//   var imageCalendar = Ti.UI.createImageView({
//     image: IMG_BASE + 'tab2.png',
//     left: 70, bottom: 2,
//     width: 32, height: 32
//   });
//   row.add(imageCalendar);

//   var labelDate = Ti.UI.createLabel({
//     color: '#999',
//     font: {fontFamily:'Arial', fontSize: defaultFontSize, fontWeight: 'normal'},
//     text: 'on ' + randomInt(30) + ' Nov 2012',
//     left: 105, bottom: 10,
//     width: 200, height: 20
//   });
//   row.add(labelDate);

//   tableData.push(row);
// }

// var tableView = Ti.UI.createTableView({
//   backgroundColor: 'white',
//   data: tableData
// });

// win.add(tableView);

win.open();
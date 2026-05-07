/**
 * ViewClone Module für Titanium Android
 * Klonen von Ti.UI.Views mit allen Properties und Kind-Views
 *
 * Marc Bender
 * Licensed under the terms of the Apache Public License
 */
package de.marcbender.viewclone;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.KrollModule;
import org.appcelerator.kroll.KrollProxy;
import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.titanium.TiApplication;
import org.appcelerator.titanium.proxy.TiViewProxy;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.kroll.common.TiConfig;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import java.lang.reflect.Constructor;


@Kroll.module(name = "ViewClone", id = "de.marcbender.viewclone")
public class ViewCloneModule extends KrollModule
{
	private static final String LCAT = "ViewCloneModule";
	private static final boolean DBG = TiConfig.LOGD;

	public ViewCloneModule()
	{
		super();
	}

	@Kroll.onAppCreate
	public static void onAppCreate(TiApplication app)
	{
		Log.d(LCAT, "Module initialized");
	}

	// Cache für Constructor-Objekte - Vermeidet wiederholte Reflection-Aufrufe
	// ConcurrentHashMap ermöglicht thread-sicheren Lesezugriff ohne externe Sperren
	private static final Map<Class<? extends TiViewProxy>, Constructor<? extends TiViewProxy>> CONSTRUCTOR_CACHE
		= new ConcurrentHashMap<>();

	// Set zur Erkennung von Zirkelbeziehungen - Vermeidet StackOverflow bei recursive references
	// ReentrantLock schützt clearCache() vor Race-Condition mit cloneProxy()-Aufrufen
	// clearCache() ersetzt das Set (statt es zu leeren), damit parallele Clones das neue
	// Set sehen, aber ihr Entry im alten Set korrekt im finally-Block entfernen können.
	private static Set<TiViewProxy> CLONING_IN_PROGRESS = new HashSet<>();
	private static final ReentrantLock CLONING_LOCK = new ReentrantLock();

	@Kroll.method
	public TiViewProxy cloneView(TiViewProxy proxy)
	{
		// Input-Validierung
		if (proxy == null) {
			Log.e(LCAT, "cloneView: proxy is null");
			return null;
		}

		if (!(proxy instanceof TiViewProxy)) {
			Log.e(LCAT, "Invalid proxy type: " + proxy.getClass().getName());
			return null;
		}

		Log.d(LCAT, "Cloning view: " + proxy.getClass().getSimpleName());

		try {
			return cloneProxy(proxy);
		} catch (Exception e) {
			Log.e(LCAT, "Error cloning view: " + e.getMessage(), e);
			return null;
		}
	}

	/**
	 * Internes rekursives Klonen eines TiViewProxy.
	 *
	 * Erstellt eine neue Instanz desselben Proxy-Typs, kopiert die Properties
	 * und klont die Kinder rekursiv. Der V8 JavaScript-Wrapper wird automatisch
	 * erstellt, wenn der Proxy an JavaScript zurückgegeben wird.
	 *
	 * Zirkelbeziehungs-Erkennung: Vermeidet StackOverflow bei recursive references
	 * (z.B. wenn ein Child eine Referenz auf den Parent enthält).
	 */
	private TiViewProxy cloneProxy(TiViewProxy proxy) throws Exception
	{
		Log.d(LCAT, "Creating new instance of: " + proxy.getClass().getSimpleName());

		// Zirkelbeziehungs-Erkennung - Vermeidet StackOverflow
		// Lock schützt clearCache() vor dem Löschen des Sets während eines Clones
		// Das Set-Referenz wird im finally aktualisiert, damit clearCache() sofort
		// das neue Set sieht, aber der alte Eintrag korrekt entfernt wird.
		CLONING_LOCK.lock();
		try {
			if (CLONING_IN_PROGRESS.contains(proxy)) {
				Log.w(LCAT, "Circular reference detected: " + proxy.getClass().getSimpleName() + " - skipping");
				return null;
			}
			CLONING_IN_PROGRESS.add(proxy);
		} finally {
			CLONING_LOCK.unlock();
		}

		try {
			// Constructor aus Cache holen oder erstellen
			Constructor<? extends TiViewProxy> constructor = getConstructor(proxy.getClass());

			// Neue Proxy-Instanz aus dem cached Constructor erstellen
			TiViewProxy clonedProxy = constructor.newInstance();

			// Properties kopieren und den Proxy initialisieren
			KrollDict props = proxy.getProperties();
			if (props != null && !props.isEmpty()) {
				KrollDict propsCopy = new KrollDict(props);
				clonedProxy.handleCreationArgs(null, new Object[] { propsCopy });
			} else {
				clonedProxy.handleCreationArgs(null, new Object[0]);
			}

			// Creation-URL übernehmen
			if (proxy.getCreationUrl() != null && proxy.getCreationUrl().url != null) {
				clonedProxy.setCreationUrl(proxy.getCreationUrl().url);
			}

			// Kinder rekursiv klonen
			TiViewProxy[] children = proxy.getChildren();
			if (children != null && children.length > 0) {
				for (TiViewProxy child : children) {
					if (child == null) {
						continue;
					}
					TiViewProxy clonedChild = cloneProxy(child);
					if (clonedChild != null) {
						clonedProxy.add(clonedChild);
					} else {
						Log.w(LCAT, "Failed to clone child: " + child.getClass().getSimpleName() + " (circular or error)");
					}
				}
			}

			return clonedProxy;

		} finally {
			// Vom Set entfernen - Freigabe für zukünftige Clones
			// Lock schützt davor, dass clearCache() das Set zwischen Prüfen und Entfernen ersetzt
			CLONING_LOCK.lock();
			try {
				CLONING_IN_PROGRESS.remove(proxy);
			} finally {
				CLONING_LOCK.unlock();
			}
		}
	}

	/**
	 * Thread-sicherer Constructor-Caching-Mechanismus.
	 * Vermeidet wiederholte Reflection-Aufrufe für den gleichen Proxy-Typ.
	 *
	 * @param proxyClass Die Proxy-Klasse
	 * @return Der cached Constructor
	 * @throws Exception Falls Constructor nicht erstellt werden kann
	 */
	@SuppressWarnings("unchecked")
	private Constructor<? extends TiViewProxy> getConstructor(Class<? extends TiViewProxy> proxyClass) throws Exception {
		// Cache lookup
		Constructor<? extends TiViewProxy> constructor = (Constructor<? extends TiViewProxy>)(Object) CONSTRUCTOR_CACHE.get(proxyClass);

		if (constructor == null) {
			// Constructor erstellen und im Cache speichern
			constructor = (Constructor<? extends TiViewProxy>)(Object) proxyClass.getDeclaredConstructor();
			CONSTRUCTOR_CACHE.put(proxyClass, constructor);
			Log.d(LCAT, "Cached new constructor for: " + proxyClass.getSimpleName());
		}

		return constructor;
	}

	/**
	 * Gibt die Memory-Caches frei.
	 * Sollte bei App-Close oder Speichermangel aufgerufen werden.
	 * Lock schützt vor Race-Condition: Falls ein paralleler Clone-Vorgang
	 * mitten in cloneProxy() läuft, wird CLONING_IN_PROGRESS neu initialisiert
	 * statt gelöscht — so bleibt die Zirkel-Erkennung stabil.
	 */
	@Kroll.method
	public void clearCache() {
		CONSTRUCTOR_CACHE.clear();
		CLONING_LOCK.lock();
		try {
			CLONING_IN_PROGRESS = new HashSet<>();
			Log.d(LCAT, "Cloning-in-progress set replaced (lock-held clones unaffected)");
		} finally {
			CLONING_LOCK.unlock();
		}
		Log.d(LCAT, "Memory caches cleared");
	}

	/**
	 * Gibt die Anzahl der gecachten Constructor-Objekte zurück.
	 *
	 * @return Anzahl der Einträge im Constructor-Cache
	 */
	@Kroll.method
	public int getCacheSize() {
		return CONSTRUCTOR_CACHE.size();
	}
}
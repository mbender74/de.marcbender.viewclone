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
import org.appcelerator.kroll.KrollObject;
import org.appcelerator.kroll.KrollProxy;
import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.titanium.TiApplication;
import org.appcelerator.titanium.proxy.TiViewProxy;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.kroll.common.TiConfig;

import android.app.Activity;
import java.lang.ref.WeakReference;

import java.lang.reflect.Constructor;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;


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

	/**
	 * Klonen einer TiUIView mit allen Properties und Kind-Views.
	 *
	 * Erstellt einen neuen Proxy desselben Typs wie das Original, kopiert
	 * alle Properties und klont die Kinder rekursiv.
	 *
	 * WICHTIG: In JavaScript verwenden Sie .children (Property), nicht .getChildren() (Methode).
	 * Beispiel: const children = clonedView.children;
	 *
	 * @param proxy Das zu klonende TiViewProxy
	 * @return Das geklonte TiViewProxy oder null bei Fehler
	 */
	// Cache für Constructor-Objekte - Vermeidet wiederholte Reflection-Aufrufe
	// WeakReference-Map ermöglicht GC des Caches wenn nötig
	private static final Map<Class<? extends TiViewProxy>, Constructor<? extends TiViewProxy>> CONSTRUCTOR_CACHE 
		= new ConcurrentHashMap<>();

	// WeakReference für Activity-Referenzen - Vermeidet Memory-Leaks durch starke References
	private final Map<TiViewProxy, WeakReference<Activity>> activityRefCache = new ConcurrentHashMap<>();

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
	 */
	private TiViewProxy cloneProxy(TiViewProxy proxy) throws Exception
	{
		Log.d(LCAT, "Creating new instance of: " + proxy.getClass().getSimpleName());
		
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

		// Activity als WeakReference speichern - Vermeidet Memory-Leaks
		Activity activity = proxy.getActivity();
		if (activity != null) {
			activityRefCache.put(clonedProxy, new WeakReference<>(activity));
			clonedProxy.setActivity(activity);
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
					Log.w(LCAT, "Failed to clone child: " + child.getClass().getSimpleName());
				}
			}
		}

		return clonedProxy;
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
	 */
	@Kroll.method
	public void clearCache() {
		CONSTRUCTOR_CACHE.clear();
		activityRefCache.clear();
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
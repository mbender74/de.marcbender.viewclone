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
	@Kroll.method
	public TiViewProxy cloneView(TiViewProxy proxy)
	{
		if (proxy == null) {
			Log.e(LCAT, "cloneView: proxy is null");
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
		// Neue Proxy-Instanz desselben Typs erstellen
		TiViewProxy clonedProxy = (TiViewProxy) proxy.getClass().getDeclaredConstructor().newInstance();

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

		// Activity übernehmen
		Activity activity = proxy.getActivity();
		if (activity != null) {
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
				}
			}
		}

		return clonedProxy;
	}
}
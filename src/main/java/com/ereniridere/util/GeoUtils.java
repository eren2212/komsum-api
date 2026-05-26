package com.ereniridere.util;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;

/**
 * Lat/Lng (API tarafı) ile JTS {@link Point} (DB / PostGIS tarafı) arasındaki dönüşümü
 * tek noktada toplayan yardımcı sınıf (DRY).
 *
 * <p>
 * 🚨 EN KRİTİK KONVANSİYON: JTS koordinat sırası (X, Y) = (longitude, latitude)'dur.
 * Yani enlem (latitude) Y'ye, boylam (longitude) X'e gider. Burada tek yerde topladığımız
 * için projenin geri kalanında bu karışıklık riski ortadan kalkar.
 * </p>
 *
 * <p>
 * {@link GeometryFactory} thread-safe olduğu için statik tek örnek paylaşılır.
 * SRID 4326 (WGS84) tüm konumlar için sabittir.
 * </p>
 */
public final class GeoUtils {

	public static final int SRID_WGS84 = 4326;

	private static final GeometryFactory FACTORY = new GeometryFactory(new PrecisionModel(), SRID_WGS84);

	private GeoUtils() {
		// Yardımcı sınıf — örneklenmez.
	}

	/**
	 * lat/lng değerlerinden SRID 4326'lı bir {@link Point} üretir.
	 *
	 * @return İkisinden biri {@code null} ise {@code null} döner (opsiyonel konumlar için).
	 */
	public static Point toPoint(Double latitude, Double longitude) {
		if (latitude == null || longitude == null) {
			return null;
		}
		// DİKKAT: Coordinate(x=longitude, y=latitude)
		return FACTORY.createPoint(new Coordinate(longitude, latitude));
	}

	/** Point'ten enlemi (latitude = Y) çıkarır. Point null ise null. */
	public static Double getLatitude(Point point) {
		return point == null ? null : point.getY();
	}

	/** Point'ten boylamı (longitude = X) çıkarır. Point null ise null. */
	public static Double getLongitude(Point point) {
		return point == null ? null : point.getX();
	}
}

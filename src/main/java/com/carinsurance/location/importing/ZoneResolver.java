package com.carinsurance.location.importing;

import java.util.Set;
import org.springframework.stereotype.Component;

/** Decides the pricing zone of a locality. Zone names match the rows inserted by migration V7. */
@Component
public class ZoneResolver {

    public static final String CABA = "CABA";
    public static final String GBA = "GBA";
    public static final String LARGE_INTERIOR_CITIES = "Ciudades grandes del interior";
    public static final String REST_OF_COUNTRY = "Resto del país";

    private static final String CABA_NAME = "Ciudad Autonoma de Buenos Aires";

    private static final Set<String> GBA_POSTAL_CODES = Set.of(
            "1602",
            "1603",
            "1605",
            "1636",
            "1638", // Vicente López
            "1607",
            "1609",
            "1640",
            "1642",
            "1643",
            "1644", // San Isidro
            "1646", // San Fernando
            "1611",
            "1617",
            "1618",
            "1621",
            "1648",
            "1670", // Tigre
            "1612",
            "1613",
            "1614",
            "1615",
            "1667", // Malvinas Argentinas
            "1665", // José C. Paz
            "1659",
            "1661",
            "1663", // San Miguel
            "1650",
            "1651",
            "1653",
            "1655",
            "1657",
            "1672", // General San Martín
            "1674",
            "1676",
            "1678",
            "1682",
            "1684",
            "1688",
            "1702", // Tres de Febrero
            "1686", // Hurlingham
            "1706",
            "1708",
            "1712", // Morón
            "1713",
            "1714", // Ituzaingó
            "1716",
            "1718",
            "1722",
            "1723", // Merlo
            "1664",
            "1742",
            "1744",
            "1746", // Moreno
            "1704",
            "1752",
            "1754",
            "1755",
            "1757",
            "1759",
            "1761",
            "1763",
            "1765",
            "1766",
            "1768",
            "1770",
            "1778", // La Matanza
            "1773",
            "1828",
            "1832",
            "1834",
            "1836", // Lomas de Zamora
            "1776",
            "1838",
            "1842", // Esteban Echeverría
            "1802",
            "1804",
            "1806", // Ezeiza
            "1822",
            "1824",
            "1825",
            "1826", // Lanús
            "1870",
            "1871",
            "1872",
            "1874",
            "1875", // Avellaneda
            "1876",
            "1878",
            "1879",
            "1881",
            "1882", // Quilmes
            "1884",
            "1885",
            "1886",
            "1890",
            "1893", // Berazategui
            "1888",
            "1889",
            "1891", // Florencio Varela
            "1846",
            "1847",
            "1849",
            "1852",
            "1854",
            "1856" // Almirante Brown
            );

    private static final Set<String> LARGE_INTERIOR_CITY_POSTAL_CODES = Set.of(
            "1894",
            "1896",
            "1897",
            "1900", // Gran La Plata
            "2000", // Rosario
            "3000", // Santa Fe
            "4000", // San Miguel de Tucumán
            "4400", // Salta
            "5000", // Córdoba
            "5500", // Mendoza
            "7600", // Mar del Plata
            "8300" // Neuquén
            );

    public String zoneName(LocalityRow row) {
        if (CABA_NAME.equals(row.province()) || CABA_NAME.equals(row.name())) {
            return CABA;
        }
        if (GBA_POSTAL_CODES.contains(row.postalCode())) {
            return GBA;
        }
        if (LARGE_INTERIOR_CITY_POSTAL_CODES.contains(row.postalCode())) {
            return LARGE_INTERIOR_CITIES;
        }
        return REST_OF_COUNTRY;
    }
}

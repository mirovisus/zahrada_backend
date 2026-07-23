package upce.fei.garden.service;

import org.springframework.data.jpa.domain.Specification;
import upce.fei.garden.model.Demand;
import upce.fei.garden.model.enums.DemandStatus;

import java.util.List;

/**
 * Sestavuje {@link Specification} pro veřejný katalog poptávek (viz {@link DemandService#getCatalog}).
 * Držena mimo {@link DemandService}, aby servisní vrstva obsahovala pouze business logiku.
 */
final class DemandSpecifications {

    private DemandSpecifications() {
    }

    /**
     * Sestaví specifikaci pro katalog: vždy omezuje na poptávky ve stavu {@link DemandStatus#NOVA}
     * (to jediné mohou zahradníci vidět) a k tomu volitelně přidá filtry podle města, typů služeb
     * a fulltextové hledání v názvu/popisu. Prázdné/{@code null} filtry se do dotazu nepřidávají.
     */
    static Specification<Demand> catalog(String city, List<Long> serviceTypeIds, String search) {
        Specification<Demand> specification = hasStatus(DemandStatus.NOVA);

        if (city != null && !city.isBlank()) {
            specification = specification.and(hasCity(city));
        }
        if (serviceTypeIds != null && !serviceTypeIds.isEmpty()) {
            specification = specification.and(hasAnyServiceType(serviceTypeIds));
        }
        if (search != null && !search.isBlank()) {
            specification = specification.and(matchesSearch(search));
        }

        return specification;
    }

    private static Specification<Demand> hasStatus(DemandStatus status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    // cesta garden.address.city funguje, protoze Address je @Embeddable v ramci Garden
    private static Specification<Demand> hasCity(String city) {
        return (root, query, cb) ->
                cb.equal(cb.lower(root.get("garden").get("address").get("city")), city.trim().toLowerCase());
    }

    private static Specification<Demand> hasAnyServiceType(List<Long> serviceTypeIds) {
        return (root, query, cb) -> {
            query.distinct(true);
            return root.join("serviceTypes").get("id").in(serviceTypeIds);
        };
    }

    private static Specification<Demand> matchesSearch(String search) {
        return (root, query, cb) -> {
            String pattern = "%" + search.trim().toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("title")), pattern),
                    cb.like(cb.lower(root.get("description")), pattern));
        };
    }
}

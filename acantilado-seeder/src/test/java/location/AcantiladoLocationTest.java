package location;

import com.acantilado.collection.location.AcantiladoLocation;
import com.acantilado.core.administrative.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Geometry;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AcantiladoLocationTest {
    @Mock
    private AyuntamientoDAO ayuntamientoDAO;

    @Mock
    private CodigoPostalDAO codigoPostalDAO;

    @Mock
    private BarrioDAO barrioDAO;

    @Mock Geometry geometry;

    private final Ayuntamiento ayuntamiento = new Ayuntamiento(
            "100",
            "someAyuntamiento",
            "12",
            "3",
            geometry);
    private final CodigoPostal codigoPostal = new CodigoPostal("someCodigoIne", "someCodigoPostal", geometry);
    private final Barrio barrio = new Barrio("someBarrio", "100", geometry);

    private AcantiladoLocation location;

    @BeforeEach
    void beforeEach() {
        when(ayuntamientoDAO.findById("100")).thenReturn(Optional.of(ayuntamiento));
        when(ayuntamientoDAO.findById("101")).thenReturn(Optional.empty());

        when(codigoPostalDAO.findById("someCodigoIne")).thenReturn(Optional.of(codigoPostal));
        when(codigoPostalDAO.findById("noneCodigoIne")).thenReturn(Optional.empty());

        when(barrioDAO.findById(1111L)).thenReturn(Optional.of(barrio));
        when(barrioDAO.findById(2L)).thenReturn(Optional.empty());

        barrio.setId(1111L);
    }

//    @Test
//    void buildsLocationIdentifier() {
//        location = new AcantiladoLocation(ayuntamiento, codigoPostal, barrio);
//        Assertions.assertEquals("100-someCodigoIne-1111", location.getIdentifier());
//    }
//
//    @Test
//    void buildsLocationIdentifierWithoutBarrio() {
//        location = new AcantiladoLocation(ayuntamiento, codigoPostal);
//        Assertions.assertEquals("100-someCodigoIne-XXX", location.getIdentifier());
//    }
//
//    @Test
//    void extractsFromLocationIdentifier() {
//        location = AcantiladoLocation.fromLocationIdentifier("100-someCodigoIne-1111", ayuntamientoDAO, codigoPostalDAO, barrioDAO);
//
//        Assertions.assertEquals("100-someCodigoIne-1111", location.getIdentifier());
//    }
//
//    @Test
//    void extractsFromLocationIdentifierThrowsIllegalArgumentException() {
//        Assertions.assertThrows(
//                IllegalArgumentException.class, () -> AcantiladoLocation.fromLocationIdentifier(
//                        "notANumber-someCodigoPostal-1111", ayuntamientoDAO, codigoPostalDAO, barrioDAO)
//        );
//    }
//
//    @Test
//    void extractsFromLocationIdentifierThrowsInvalidStateException() {
//        Assertions.assertThrows(
//                IllegalStateException.class, () -> AcantiladoLocation.fromLocationIdentifier(
//                        "101-someCodigoIne-1111", ayuntamientoDAO, codigoPostalDAO, barrioDAO)
//        );
//    }
}
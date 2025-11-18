# Spanish Topology and Real Estate Locations

## Topology Overview

in Spain, the highest level distinction between regions is called the `Comunidad Autonoma`, which consist of `Provincias` that consist of `Ayuntamientos`. Acantilado currently has `Provincias` linked to `Ayuntamientos`, each with geometry data included. `Provincias` contain an ID for their `Comunidad Autonoma` but there's no specific entity for it. Unfortunately, we don't currently have `Provincias` for INE codes `35` and `38` because they aren't in the dataset we use.

Acantilado also has `Codigos Postales` for the entirety of Spain. These have many-to-many mapping to `Ayuntamientos`: in cities there are often multiple postcodes for one `Ayuntamiento`, and in rural areas multiple `Ayuntamientos` are covered by a single `Codigo Postal`. Finally, we have `Barrios` for the largest `Ayuntamientos`, e.g. city centers.

## Acantilado Location ID

There is a concept of Acantilado Location ID which is intended for geographical comparative analysis. It's made up of:

```
[AYUNTAMIENTO ID] - [POSTCODE ID] - [BARRIO or XXX]
```

Because datasets don't perfectly overlap with Idealista, Acantilado builds N:N mappings from its `Ayuntamientos` to Idealista Location IDs. These mappings are used to ensure we give listings the right Acantilado Location ID. While the mappings are automatically built from scratch, they do require some manual involvement, so should be imported from disk instead.







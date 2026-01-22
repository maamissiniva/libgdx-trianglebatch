# Libgdx triangle batch

!!! In progress !!!


Experiment on

  - how to build some LibGDX triangle output system.
  - shader algorithm selection through vertex data

The shader strategy may be a bad idea as

  - it uses conditionals which may be pretty bad for GPUs
  - it uses more GPU bandwidth than LibGDX SpritBatch

## Shader algorithms

The shader implementation uses per vertex color. 

### Draw colored

No texture used, vertices use associated color.

### Draw distanced outilne

Uses the texture as a signed distance field to render some kind of outline.

### Draw distanced

Uses the texture as a signed distance field to render anti-aliased images/fonts.

### Draw textured

Uses the texture.

## Usage

The `begin()` and `end()` method must be used to delimit the output.

Between `begin()` and `end()` the following methods are provided:

  - drawColoredTriangle
  - drawColoredQuad
  - drawDistancedOutlineTriangle
  - drawDistancedOutlineQuad
  - drawDistancedTriangle
  - drawDistancedQuad
  - drawTexturedTriangle
  - drawTexturedQuad



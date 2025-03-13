## Enunciado
Diseñar un sistema multiagente que permita la compra/venta de libros mediante el procedimiento de subasta al alza (english auction).
En este caso se entiende que hay un vendedor que posee uno o más libros a la venta y múltiples compradores interesados en alguno de sus libros.

Los compradores deben de implementar un valor máximo por el cual están dispuestos a pujar y el vendedor el paso o incremento entre dos pujas sucesivas.
Con el fin de poder apreciar mejor el funcionamiento, asúmase que entre dos pujas sucesivas debe transcurrir un tiempo de 10 segundos, por lo que el
vendedor deberá esperar ese tiempo antes de asignar un nuevo precio al libro. La subasta concluirá cuando en una ronda todos los posibles compradores indican
que no están interesados en el libro, asignándose el mismo al primer comprador que haya pujado por el mismo en la ronda anterior, o bien cuando en la roda actual exista un único comprador interesado.

## Requisitos
Se deberá de crear un programa comprador y otro programa vendedor. Deberá tener:
- Un vendedor, múltiples compradores (posiblemente interesados en varias subastas) que entran y salen dinámicamente en la subasta.
- Múltiples subastas simultaneas.
- Interfaz gráfica en el vendedor para iniciar la subasta de nuevos libros y el seguimiento de cada una de las subastas activas.
- Interfaz gráfica en el comprador ilustrando el estado de la subasta o subastas en las que esté interesado.

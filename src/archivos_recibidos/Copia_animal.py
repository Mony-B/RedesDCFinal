class Animal:
    def __init__(self):
        self.comida='COME ALGO'
        self.esta_vivo=True
    def  come(self):
        print ('Estoy comiendo', self.comida)
#crea un objeto de tipo animal y le digo que coma
firulais = Animal()
firulais.come()
firulais.tipo_alimentacion='Croqutas PURINA'
firulais.come()

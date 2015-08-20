import play.api.test.WithApplicationLoader

class MyAppLoader extends WithApplicationLoader(new CustomApplicationLoader)

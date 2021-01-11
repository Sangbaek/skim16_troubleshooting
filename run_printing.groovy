import org.jlab.jnp.hipo4.data.Event;
import org.jlab.jnp.hipo4.data.Bank
import org.jlab.jnp.hipo4.io.HipoReader
import org.jlab.io.hipo.HipoDataEvent
import org.jlab.io.hipo.HipoDataSource
import org.jlab.groot.data.TDirectory
import org.jlab.jnp.hipo4.data.SchemaFactory
import org.jlab.jnp.hipo4.data.Schema
import wagon.DVCSWagon
/////////////////////////////////////////////////////////////////////

// def problematicEvents = [45040104, 45167466, 90664027, 45282893, 27046060, 27196869, 69221029, 41957238, 139439623, 132085813, 60462487, 107004602, 107025707, 71579058, 128424051, 85423888, 50002416, 93204648, 29114644, 144625985, 32764272, 81704849, 79539897, 46208798, 46297015, 46460162, 46555534, 86511011, 81850085, 81988334]
def problematicEvent = 46208798

args.each{fname->
  def reader = new HipoReader()
  reader.open(fname)
  SchemaFactory schema = reader.getSchemaFactory();
  Schema recParticle = schema.getSchema("REC::Particle")
  Schema runConfig = schema.getSchema("RUN::config")
  def particle = new Bank(recParticle)
  def config = new Bank(runConfig)
  def jnp_event = new org.jlab.jnp.hipo4.data.Event()
  def dvcswagon = new DVCSWagon()

  while(reader.hasNext()) {
    reader.nextEvent(jnp_event)
    particle = jnp_event.read(particle)
    config = jnp_event.read(config)
    def eventNumber = config.getInt("event", 0)
    if (eventNumber == problematicEvent){
      println("event "+eventNumber+ " is detected in the file " + fname)
      particle.show()
      if (dvcswagon.processDataEvent(jnp_event, schema)) println("It is a DVCS candidate!")
    }
  }
  reader.close()
}
<p align="center">
  <img src="https://www.corda.net/wp-content/uploads/2016/11/fg005_corda_b.png" alt="Corda" width="500">
</p>
<h1>Energy Account CorDapp</h1>
<h4>Quick start guide</h4>
The gradlew and runnodes scripts referred to in the below steps have two variants.
Run the <code>.bat</code> versions on Windows, and the scripts without an extension on Linux.
<br /><br />
<ol>
<li>Clone this repo</li>
<li>Run <code>gradlew clean prepareLocalNodes</code></li>
<li>Go to <code>build/nodes</code> and execute<code>runnodes</code>.
    This will bring up your Corda nodes.<b>Make sure your nodes have successfully
    started before continuing.</b> You can verify this by looking for the following
    in the console:<br/>
    <code>Node for ... started up and registered in ... sec</code><br/>
    If this doesn't happen, yoy may need to hit enter to get the shell to
    move on, as it can sometimes get stuck. You should have 4 nodes running in
    total; 2 suppliers, the government regulator, and the notary.</li>
<li>Run <code>gradlew runBritishEnergyWebCli</code> and
    <code>gradlew runUKPowerWebCli</code> which will start your webservers.
    Note these run synchronously, so you will need to terminate the gradle tasks
    manually to stop the webservers.</li>
<li>Go to <a href="http://localhost:10050">http://localhost:10050</a>
    Each supplier has a customer and supplier portal. You will need to
    create accounts via the supplier portal, before being able to
    lookup accounts using account ID via the customer portal.</li>
</ol>
To terminate your Corda nodes, execute <code>bye</code> within each nodes console.


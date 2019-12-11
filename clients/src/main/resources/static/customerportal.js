"use strict";

let Alert = ReactBootstrap.Alert;
let Accordion = ReactBootstrap.Accordion;
let Button = ReactBootstrap.Button;
let Card = ReactBootstrap.Card;
let Col = ReactBootstrap.Col;
let Container = ReactBootstrap.Container;
let Form = ReactBootstrap.Form;
let InputGroup = ReactBootstrap.InputGroup;
let Modal = ReactBootstrap.Modal;
let NavBar = ReactBootstrap.Navbar;
let Row = ReactBootstrap.Row;
let Tab = ReactBootstrap.Tab;
let Tabs = ReactBootstrap.Tabs;

function getIdentityDisplayName(X500Name) {
    return X500Name.match("(?<=O=).*(?=, L=)")[0]
}

class NavigationBar extends React.Component {
    constructor(props) {
        super(props);
    }

    name() {
        if (this.props.nodeInfo.legalIdentitiesAndCerts) {
            return getIdentityDisplayName(this.props.nodeInfo.legalIdentitiesAndCerts[0])
        }

        return "";
    }

    render() {
        return (
            <NavBar className="bg-light justify-content-between mb-3">
                <NavBar.Brand>
                    <b>{this.name()}</b> Customer Portal
                </NavBar.Brand>
                <NavBar.Text>
                    powered by&nbsp;
                    <a href='https://www.r3.com/'>
                        <img src='/corda.png'
                             height="30"
                             className="d-inline-block align-top"
                             alt='corda'/>
                     </a>
                </NavBar.Text>
            </NavBar>
        )
    }
}

class AccountFinder extends React.Component {
    constructor(props) {
        super(props);
        this.state = {
            submitIsEnabled: false
        }

        this.handleIdChange = this.handleIdChange.bind(this);
        this.handleSubmit = this.handleSubmit.bind(this);
    }

    render() {
        return (
            <Card>
                <Card.Header as="h6">Account Finder</Card.Header>
                <Card.Body>
                    <Form onSubmit={this.handleSubmit}>
                        <Form.Row>
                        <Form.Control className="accountId"
                                      name="accountId"
                                      ref="accountId"
                                      placeholder='Enter your 36 character account id'
                                      onChange={this.handleIdChange}/>
                        <Button variant="dark"
                                type='submit'
                                disabled={!this.state.submitIsEnabled}>Submit</Button>
                        </Form.Row>
                        <Form.Row>
                            <Alert variant="danger" show={this.props.lookupState=="FAIL"}>
                                   Could not find account
                            </Alert>
                        </Form.Row>
                    </Form>
                </Card.Body>
            </Card>
        )
    }

    handleIdChange(event) {
        let accountId = event.target.value;
        this.setState({submitIsEnabled:
            accountId.match(
                /^[0-9A-F]{8}-[0-9A-F]{4}-4[0-9A-F]{3}-[89AB][0-9A-F]{3}-[0-9A-F]{12}$/i)});
    }

    handleSubmit(event) {
        let accountId = event.target.elements.accountId.value
        event.preventDefault();
        this.setState({accountId: accountId});
        this.props.onSubmit(accountId)
    }
}

class AccountPersonalDetails extends React.Component {
    constructor(props) {
        super(props)
    }

    render() {
        return (
            <Card>
            <Card.Body>
            <Form>
                <Form.Group as={Row}>
                    <Form.Label xs="auto"
                                className="font-weight-bolder"
                                column
                                span="false">Account Id
                    </Form.Label>
                    <Col>
                        <Form.Control className="accountId"
                                      disabled
                                      plaintext
                                      value={this.props.accountId}/>
                    </Col>
                </Form.Group>
                <Form.Group as={Row}>
                    <Form.Label xs="auto"
                                className="font-weight-bolder"
                                column
                                span="false">First Name
                    </Form.Label>
                    <Col>
                        <Form.Control className="nameField"
                                      disabled
                                      plaintext
                                      value={this.props.firstName}/>
                    </Col>
                </Form.Group>
                <Form.Group as={Row}>
                    <Form.Label xs="auto"
                                className="font-weight-bolder"
                                column
                                span="false">Last Name
                    </Form.Label>
                    <Col>
                        <Form.Control className="nameField"
                                      disabled
                                      plaintext
                                      value={this.props.lastName}/>
                    </Col>
                </Form.Group>
            </Form>
            </Card.Body>
            </Card>
        )
    }
}

class AccountTransferPanel extends React.Component {
    constructor(props) {
        super(props)

        this.handleSubmit = this.handleSubmit.bind(this);
    }

    render() {
        return (
            <Card>
                <Card.Body>
                    <Form inline onSubmit={this.handleSubmit}>
                        <Form.Row>
                            <Form.Label className="font-weight-bolder mr-2">
                                Transfer account to
                            </Form.Label>
                            <Form.Control as="select"
                                          name="newSupplier"
                                          ref="newSupplier">
                                {
                                    this.props.otherSuppliers.map((it) =>
                                        <option label={getIdentityDisplayName(
                                                it.legalIdentitiesAndCerts[0])}>{
                                            it.legalIdentitiesAndCerts[0]}</option>)
                                }
                            </Form.Control>
                            <Button variant="dark"
                                    type='submit'>Change Supplier</Button>
                        </Form.Row>
                    </Form>
                </Card.Body>
            </Card>
        )
    }

    handleSubmit(event) {
        let newSupplier = event.target.elements.newSupplier.value
        event.preventDefault();
        this.props.onSubmit(this.props.accountId, newSupplier)
    }
}

class AccountTransferResultDialog extends React.Component {
    constructor(props) {
        super(props);
        this.handleClose = this.handleClose.bind(this);
    }

    render() {
        let modalText =
            ( this.props.transferResult == "OK"
              ?
              "Account transfer was successful. You will now be logged out. " +
              "Please access your account on your new suppliers website."
              :
              "Account transfer failed. Please contact customer services."
        )

        return (
            <div class="panel panel-default">
                <Modal show={this.props.transferResult != ""}
                       onHide={this.handleClose}>
                    <Modal.Header closeButton>
                        <Modal.Title>Transfer Account</Modal.Title>
                    </Modal.Header>
                        <Modal.Body>
                            {modalText}
                        </Modal.Body>
                    <Modal.Footer>
                        <Button onClick={this.handleClose}>
                            Close
                        </Button>
                    </Modal.Footer>
                </Modal>
            </div>
        )
    }

    handleClose() {
        event.preventDefault();
        this.props.onClose();
    }
}

class App extends React.Component {
    constructor(props) {
        super(props);

        this.state = {
            nodeInfo: {},
            networkMap: {},
            otherSuppliers: [],
            lookupState: "",
            transferResult: "",
            accountDetails: {}
        };

        this.getNodeInfo = this.getNodeInfo.bind(this);
        this.getNetworkMap = this.getNetworkMap.bind(this);
        this.getOtherSuppliers = this.getOtherSuppliers.bind(this);
        this.getAccount = this.getAccount.bind(this);
        this.transferAccount = this.transferAccount.bind(this);
        this.onTransferAccountClose = this.onTransferAccountClose.bind(this)
    }

    componentDidMount() {
        this.getNodeInfo()
            .then(() => this.getNetworkMap())
            .then(() => this.getOtherSuppliers())
    }

    getNodeInfo() {
        return fetch('/api/nodeInfo')
            .then(result => result.json())
            .then(nodeInfo => this.setState({nodeInfo: nodeInfo}));
    }

    getNetworkMap() {
        return fetch('/api/networkMap')
            .then(result => result.json())
            .then(networkMap => this.setState({networkMap: networkMap}));
    }

    getOtherSuppliers() {
        if (!this.state.nodeInfo.legalIdentitiesAndCerts ||
            !this.state.networkMap) return;

        let thisNode = this.state.nodeInfo.legalIdentitiesAndCerts[0];

        let otherSuppliers = this.state.networkMap.filter(
            it => it.legalIdentitiesAndCerts[0] !== thisNode &&
                  !it.legalIdentitiesAndCerts[0].toLowerCase().includes('notary') &&
                  !it.legalIdentitiesAndCerts[0].toLowerCase().includes('regulator'));

        this.setState({otherSuppliers: otherSuppliers});
    }

    getAccount(accountId) {
        var request = '/api/getaccount?id=' + accountId
        return fetch(request)
            .then(result => result.json())
            .then(accountDetails => this.setState({
                lookupState: "OK",
                accountDetails: accountDetails}))
            .catch((error) => {
                console.info(error);
                this.setState({lookupState: "FAIL"})
            });
    }

    transferAccount(accountId, newSupplier) {
        return fetch('/api/transferaccount', {
            method: 'PATCH',
            headers: {
                'Accept': 'application/json',
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                accountId: accountId,
                toSupplier: newSupplier
            })
        })
            .then(result => result.text())
            .then(result => this.setState({transferResult: result}))
    }

    onTransferAccountClose() {
        if (this.state.transferResult == "OK") {
            this.setState({lookupState: ""})
        }

        this.setState({transferResult: ""})
    }

    render() {
        let account = this.state.accountDetails;

        if (this.state.lookupState != "OK") {
            return (
                <div>
                    <NavigationBar nodeInfo={this.state.nodeInfo}/>
                    <AccountFinder lookupState={this.state.lookupState}
                                   onSubmit={this.getAccount}/>
                </div>
            )
        } else {
            return (
                <div>
                    <NavigationBar nodeInfo={this.state.nodeInfo}/>
                    <Tabs defaultActiveKey="details" id="main-tabs">
                        <Tab eventKey="details" title="My Details">
                            <AccountPersonalDetails accountId={account.linearId.id}
                                                    firstName={account.firstName}
                                                    lastName={account.lastName}/>
                        </Tab>
                        <Tab eventKey="transfer" title="Transfer Account">
                            <AccountTransferPanel accountId={account.linearId.id}
                                                  otherSuppliers={this.state.otherSuppliers}
                                                  onSubmit={this.transferAccount}/>
                            <AccountTransferResultDialog transferResult={this.state.transferResult}
                                                         onClose={this.onTransferAccountClose} />
                        </Tab>
                    </Tabs>
                </div>
            )
        }
    }
}

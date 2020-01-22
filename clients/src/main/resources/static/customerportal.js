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
                             className="d-inline-block mb-1"
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
                                className="custDetailsLabel font-weight-bolder"
                                column
                                span="false">Account Id
                    </Form.Label>
                    <Col>
                        <Form.Control className="accountId"
                                      disabled
                                      plaintext
                                      value={this.props.accountDetails.linearId.id}/>
                    </Col>
                </Form.Group>
                <CustomerForm customerDetails={this.props.accountDetails.customerDetails}
                              mode="View"
                              onChange={this.onChange}/>
            </Form>
            <ButtonGroup className="mx-n1">
                <Button variant="dark"
                        onClick={this.props.onAccountModifyRequest}>Update Details</Button>
            </ButtonGroup>
            </Card.Body>
            </Card>
        )
    }
}

class AccountModifyDialog extends React.Component {
    constructor(props) {
        super(props);

        this.state = {
            customerDetails: this.props.accountDetails.customerDetails
        }

        this.onChange = this.onChange.bind(this);
        this.handleClose = this.handleClose.bind(this);
    }

    render() {
        return (
            <div class="panel panel-default">
                <Modal show={this.props.actionState == "Modify"}>
                    <Modal.Header>
                        <Modal.Title>Update Details</Modal.Title>
                    </Modal.Header>
                    <Modal.Body>
                        { this.props.actionResult != "OK" &&
                         <CustomerForm customerDetails={this.state.customerDetails}
                                       mode={this.props.actionState}
                                       onChange={this.onChange}/>
                        }
                        { this.props.actionResult == "FAIL" &&
                         <div className="text-danger">Account modification failed</div>
                        }
                        { (this.props.actionResult == "OK") &&
                         "Account was successfully modified."
                        }
                    </Modal.Body>
                    <Modal.Footer>
                        { (this.props.actionResult != "OK") &&
                            <ButtonGroup>
                                <Button variant="success"
                                        disabled={this.props.actionResult == "PENDING"}
                                        onClick={() =>
                                            this.props.onModifyConfirm(this.state.customerDetails)}>
                                    Update
                                </Button>
                                <Button variant="secondary" onClick={this.handleClose}>
                                    Cancel
                                </Button>
                            </ButtonGroup>
                        }
                        { (this.props.actionResult == "OK") &&
                              <Button onClick={this.handleClose}>
                                  Close
                              </Button>
                        }
                    </Modal.Footer>
                </Modal>
            </div>
        )
    }

    onChange(field, value) {
        this.setState(
            {customerDetails: Object.assign(
                 {},
                 this.state.customerDetails,
                 {[field]: value})
            });
    }

    handleClose() {
        this.props.onClose();
    }
}

class MeterReadPanel extends React.Component {
    constructor(props) {
        super(props)
    }

    render() {
        return (
            <Card>
            <Card.Body>
            { this.props.accountDetails.meterReadings.length == 0 &&
            <p>No meter readings exist</p>
            }
            { this.props.accountDetails.meterReadings.length != 0 &&
            <MeterReadingTable meterReadings={this.props.accountDetails.meterReadings} />
            }
            <ButtonGroup className="mx-n1">
                <Button variant="dark"
                        onClick={this.props.onMeterReadRequest}>Submit Reading</Button>
            </ButtonGroup>
            </Card.Body>
            </Card>
        )
    }
}

class MeterReadDialog extends React.Component {
    constructor(props) {
        super(props);

        this.state = {
            reading: ""
        }

        this.handleClose = this.handleClose.bind(this);
        this.handleChange = this.handleChange.bind(this);
    }

    render() {
        return (
            <div class="panel panel-default">
                <Modal show={this.props.actionState == "MeterRead"}>
                    <Modal.Header>
                        <Modal.Title>Meter Reading</Modal.Title>
                    </Modal.Header>
                    <Modal.Body>
                        { this.props.actionResult != "OK" &&
                        <Form.Group as={Row}>
                            <Form.Label xs="auto"
                                        className="font-weight-bolder"
                                        column
                                        span="false">Reading
                            </Form.Label>
                            <Col>
                                <Form.Control className="readingField"
                                              placeholder={"00000"}
                                              onChange={this.handleChange}
                                              maxLength="5"/>
                            </Col>
                        </Form.Group>
                        }
                        { this.props.actionResult == "FAIL" &&
                         <div className="text-danger">Invalid value</div>
                        }
                        { (this.props.actionResult == "OK") &&
                         "Meter reading successfully submitted."
                        }
                    </Modal.Body>
                    <Modal.Footer>
                        { (this.props.actionResult != "OK") &&
                            <ButtonGroup>
                                <Button variant="success"
                                        disabled={this.props.actionResult == "PENDING"}
                                        onClick={() =>
                                            this.props.onMeterReadSubmit(this.state.reading)}>
                                    Submit
                                </Button>
                                <Button variant="secondary" onClick={this.handleClose}>
                                    Cancel
                                </Button>
                            </ButtonGroup>
                        }
                        { (this.props.actionResult == "OK") &&
                              <Button onClick={this.handleClose}>
                                  Close
                              </Button>
                        }
                    </Modal.Footer>
                </Modal>
            </div>
        )
    }

    handleClose() {
        this.props.onClose();
    }

    handleChange(event) {
        this.setState({reading: event.target.value})
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
                    <Form onSubmit={this.handleSubmit}>
                        <Form.Row>
                            <Form.Label className="font-weight-bolder">
                                Transfer account to
                            </Form.Label>
                            <Form.Control className="my-2"
                                          as="select"
                                          name="newSupplier"
                                          ref="newSupplier">
                                {
                                    this.props.otherSuppliers.map((it) =>
                                        <option label={getIdentityDisplayName(
                                                it.legalIdentitiesAndCerts[0])}>{
                                            it.legalIdentitiesAndCerts[0]}</option>)
                                }
                            </Form.Control>
                        </Form.Row>
                        <Form.Row>
                            <Button className="mt-2"
                                    variant="dark"
                                    disabled={this.props.transferResult == "PENDING"}
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
                <Modal show={this.props.transferResult != "" &&
                             this.props.transferResult != "PENDING"}>
                    <Modal.Header>
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
        this.accountModifyRequest = this.accountModifyRequest.bind(this);
        this.meterReadRequest = this.meterReadRequest.bind(this);
        this.modifyAccount = this.modifyAccount.bind(this);
        this.meterRead = this.meterRead.bind(this);
        this.transferAccount = this.transferAccount.bind(this);
        this.onTransferAccountClose = this.onTransferAccountClose.bind(this);
        this.finishAction = this.finishAction.bind(this);
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

    accountModifyRequest(event) {
        this.setState({currentAction: "Modify" })
    }

    meterReadRequest(event) {
        this.setState({currentAction: "MeterRead" })
    }

    getAccount(accountId) {
        var request = '/api/getAccount?id=' + accountId
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

     modifyAccount(customerDetails) {
        this.setState({actionResult: "PENDING"})
        return fetch('/api/modifyAccount', {
            method: 'PATCH',
            headers: {
                'Accept': 'application/json',
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                accountId: this.state.accountDetails.linearId.id,
                customerDetails: customerDetails
            })
        })
            .then(result => result.text())
            .then(result => {
                if ( result == "OK" ) {
                    this.setState({actionResult: "OK"})
                } else {
                    this.setState({actionResult: "FAIL"})
                }
            })
    }

    meterRead(accountId, reading) {
        this.setState({actionResult: "PENDING"})
        return fetch('/api/submitMeterRead', {
            method: 'PATCH',
            headers: {
                'Accept': 'application/json',
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                accountId: accountId,
                units: reading
            })
        })
            .then(result => result.text())
            .then(result => {
                if ( result == "OK" ) {
                    this.setState({actionResult: "OK"})
                } else {
                    this.setState({actionResult: "FAIL"})
                }
            })
    }

    transferAccount(accountId, newSupplier) {
        this.setState({transferResult: "PENDING"})
        return fetch('/api/transferAccount', {
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

    finishAction() {
        this.setState({currentAction: "",
                       actionResult: ""})
        this.getAccount(this.state.accountDetails.linearId.id)
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
                            <AccountPersonalDetails accountDetails={account}
                                                    onAccountModifyRequest={this.accountModifyRequest}/>
                            <AccountModifyDialog actionState={this.state.currentAction}
                                                 actionResult={this.state.actionResult}
                                                 accountDetails={account}
                                                 onClose={this.finishAction}
                                                 onModifyConfirm={(accountDetails) =>
                                                     this.modifyAccount(accountDetails)}/>
                        </Tab>
                        <Tab eventKey="meterread" title="Meter Readings">
                            <MeterReadPanel accountDetails={account}
                                            onMeterReadRequest={this.meterReadRequest} />
                            <MeterReadDialog actionState={this.state.currentAction}
                                             actionResult={this.state.actionResult}
                                             onClose={this.finishAction}
                                             onMeterReadSubmit={(reading) =>
                                                     this.meterRead(account.linearId.id,
                                                                    reading)}/>
                        </Tab>
                        <Tab eventKey="transfer" title="Transfer Account">
                            <AccountTransferPanel accountId={account.linearId.id}
                                                  otherSuppliers={this.state.otherSuppliers}
                                                  transferResult={this.state.transferResult}
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

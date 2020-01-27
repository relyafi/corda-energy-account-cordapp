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
                    <b>{this.name()}</b> Supplier Portal
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

class AccountList extends React.Component {
    constructor(props) {
        super(props);
    }

    render() {
        return (
            <div>
            <Container fluid>
                { this.props.accounts.length > 0 &&
                <ListGroup className="mb-2 mx-n2">
                    <ListGroup.Item className="bg-light">
                        <Row className="font-weight-bolder my-n1">
                            <Col className="accountId" xs={4}>Account Id</Col>
                            <Col className="nameField">First Name</Col>
                            <Col className="nameField">Last Name</Col>
                            <Col className="dateField">Date Of Birth</Col>
                            <Col className="amountField">Current Balance</Col>
                        </Row>
                    </ListGroup.Item>
                    { this.props.accounts.map(
                        account =>
                            <ListGroup.Item action
                                            xs="auto"
                                            key={account.linearId.id}
                                            active={account.linearId.id ==
                                                this.props.activeAccountId}
                                            onClick={(id) => this.props.onAccountSelect(
                                                account.linearId.id)}>
                                <Row className="my-n1">
                                    <Col className="accountId" xs={4}>{account.linearId.id}</Col>
                                    <Col className="nameField">{account.customerDetails.firstName}</Col>
                                    <Col className="nameField">{account.customerDetails.lastName}</Col>
                                    <Col className="dateField">{account.customerDetails.dateOfBirth}</Col>
                                    <Col className="amountField">
                                        {formatCurrency(getCurrentBalance(account.billingEntries))}</Col>
                                </Row>
                            </ListGroup.Item>
                    )}

                </ListGroup>
                }
                { this.props.accounts.length == 0 &&
                <p>No accounts exist</p>
                }
                <ButtonGroup className="ml-n2">
                    <Button variant="dark"
                            onClick={this.props.onAccountCreateRequest}>Create Account</Button>
                    <Button variant="dark"
                            disabled={this.props.activeAccountId == ""}
                            onClick={this.props.onAccountModifyRequest}>Modify Account</Button>
                    <Button variant="dark"
                            disabled={this.props.activeAccountId == ""}
                            onClick={this.props.onAccountDeleteRequest}>Delete Account</Button>
                </ButtonGroup>
                <ButtonGroup className="mx-1">
                    <Button variant="dark"
                            disabled={this.props.activeAccountId == ""}
                            onClick={this.props.onBillRequest}>Generate Bill</Button>
                    <Button variant="dark"
                            disabled={this.props.activeAccountId == ""}
                            onClick={this.props.onAdjustRequest}>Adjust Balance</Button>
                </ButtonGroup>
                <ButtonGroup>
                    <Button variant="dark"
                            disabled={this.props.activeAccountId == ""}
                            onClick={this.props.onAccountViewRequest}>View Details</Button>
                </ButtonGroup>
            </Container>
            </div>
        )
    }
}

class AccountCreateDialog extends React.Component {
    constructor(props) {
        super(props);

        this.state = {
            accountDetails: { customerDetails: {} }
        }

        this.onChange = this.onChange.bind(this);
        this.handleClose = this.handleClose.bind(this);
    }

    render() {
        return (
            <div class="panel panel-default">
                <Modal show={this.props.actionState == "Create"}>
                    <Modal.Header>
                        <Modal.Title>Create Account</Modal.Title>
                    </Modal.Header>
                    <Modal.Body>
                        { this.props.actionResult != "OK" &&
                         <CustomerForm customerDetails={this.state.accountDetails.customerDetails}
                                       mode={this.props.actionState}
                                       onChange={this.onChange}/>
                        }
                        { this.props.actionResult == "FAIL" &&
                         <div className="text-danger">Account creation failed</div>
                        }
                        { (this.props.actionResult == "OK") &&
                         "Account was successfully created."
                        }
                    </Modal.Body>
                    <Modal.Footer>
                        { (this.props.actionResult != "OK") &&
                            <ButtonGroup>
                                <Button variant="success"
                                        disabled={this.props.actionResult == "PENDING"}
                                        onClick={() =>
                                            this.props.onCreateConfirm(this.state.accountDetails)}>
                                    Create
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
            {accountDetails: Object.assign(
                {},
                this.state.accountDetails,
                {customerDetails: Object.assign(
                     {},
                     this.state.accountDetails.customerDetails,
                     {[field]: value})
                })
            });
    }

    handleClose() {
        this.setState({ accountDetails: { customerDetails: {} }});
        this.props.onClose();
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
                        <Modal.Title>Modify Account</Modal.Title>
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
                                    Modify
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

class AccountDeleteDialog extends React.Component {
    constructor(props) {
        super(props);
    }

    render() {
        let modalText = ""

        switch ( this.props.actionResult ) {
        case "":
        case "PENDING":
            modalText = "Are you sure you want to delete account " +
                        this.props.accountId + "?"
            break;
        case "OK":
            modalText = "Account was successfully deleted."
            break;
        case "FAIL":
            modalText = "Account delete failed. Please contact support."
            break;
        default:
            break;
        }

        return (
            <div class="panel panel-default">
                <Modal show={this.props.actionState == "Delete"}>
                    <Modal.Header>
                        <Modal.Title>Delete Account</Modal.Title>
                    </Modal.Header>
                        <Modal.Body>
                            {modalText}
                        </Modal.Body>
                    <Modal.Footer>
                        { (this.props.actionResult == "" ||
                           this.props.actionResult == "PENDING") &&
                            <ButtonGroup>
                                <Button variant="danger"
                                        disabled={this.props.actionResult != ""}
                                        onClick={this.props.onDeleteConfirm}>
                                    Delete
                                </Button>
                                <Button variant="secondary" onClick={this.props.onClose}>
                                    Cancel
                                </Button>
                            </ButtonGroup>
                        }
                        { (this.props.actionResult == "OK" ||
                           this.props.actionResult == "FAIL") &&
                              <Button onClick={this.props.onClose}>
                                  Close
                              </Button>
                        }
                    </Modal.Footer>
                </Modal>
            </div>
        )
    }
}

class GenerateBillDialog extends React.Component {
    constructor(props) {
        super(props);

        this.handleClose = this.handleClose.bind(this);
    }

    render() {
        var promptText = "Generate a new bill for account " + this.props.accountId + "?"
        return (
            <div class="panel panel-default">
                <Modal show={this.props.actionState == "Generate"}>
                    <Modal.Header>
                        <Modal.Title>Generate Bill</Modal.Title>
                    </Modal.Header>
                    <Modal.Body>
                        { this.props.actionResult != "OK" &&
                          promptText
                        }
                        { this.props.actionResult == "FAIL" &&
                         "Bill generation failed."
                        }
                        { (this.props.actionResult == "OK") &&
                         "Bill generation successful."
                        }
                    </Modal.Body>
                    <Modal.Footer>
                        { (this.props.actionResult != "OK") &&
                            <ButtonGroup>
                                <Button variant="success"
                                        disabled={this.props.actionResult == "PENDING"}
                                        onClick={this.props.onBillSubmit}>
                                    Submit
                                </Button>
                                <Button variant="secondary" onClick={this.handleClose}>
                                    Cancel
                                </Button>
                            </ButtonGroup>
                        }
                        { (this.props.actionResult == "OK" ||
                           this.props.actionResult == "FAIL") &&
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
}

class App extends React.Component {
    constructor(props) {
        super(props);

        this.state = {
            nodeInfo: {},
            networkMap: {},
            accounts: [],
            initialised: false,
            activeAccountId: "",
            activeAccount: {},
            currentAction: "",
            actionResult: ""};

        this.getNodeInfo = this.getNodeInfo.bind(this);
        this.getNetworkMap = this.getNetworkMap.bind(this);
        this.accountCreateRequest = this.accountCreateRequest.bind(this);
        this.accountModifyRequest = this.accountModifyRequest.bind(this);
        this.accountDeleteRequest = this.accountDeleteRequest.bind(this);
        this.billRequest = this.billRequest.bind(this);
        this.adjustRequest = this.adjustRequest.bind(this);
        this.accountViewRequest = this.accountViewRequest.bind(this);
        this.accountSelect = this.accountSelect.bind(this);
        this.createAccount = this.createAccount.bind(this);
        this.modifyAccount = this.modifyAccount.bind(this);
        this.deleteAccount = this.deleteAccount.bind(this);
        this.generateBill = this.generateBill.bind(this);
        this.adjustBalance = this.adjustBalance.bind(this);
        this.finishAction = this.finishAction.bind(this);
    }

    componentDidMount() {
        this.getNodeInfo()
            .then(() => this.getNetworkMap())
            .then(() => this.getAccounts())
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

    getAccounts() {
        return fetch('/api/getAllAccounts')
            .then(result => result.json())
            .then(accounts => this.setState({initialised: true,
                                             accounts: accounts}))
    }

    accountCreateRequest(event) {
        this.setState({currentAction: "Create"})
    }

    accountModifyRequest(event) {
        this.setState({currentAction: "Modify" })
    }

    accountDeleteRequest(event) {
        this.setState({currentAction: "Delete"})
    }

    billRequest(event) {
        this.setState({currentAction: "Generate"})
    }

    adjustRequest(event) {
        this.setState({currentAction: "Adjust"})
    }

    accountViewRequest(event) {
        this.setState({currentAction: "View"})
    }

    accountSelect(id) {
        var foundAccount = false;
        for (let i = 0;
             i < this.state.accounts.length && !foundAccount;
             ++i) {
            if (this.state.accounts[i].linearId.id == id) {
                foundAccount = true;
                this.setState({activeAccountId: id,
                               activeAccount: this.state.accounts[i]})
            }
        }
    }

    createAccount(accountDetails) {
        this.setState({actionResult: "PENDING"})
        return fetch('/api/createAccount', {
            method: 'POST',
            headers: {
                'Accept': 'application/json',
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(accountDetails)
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

     modifyAccount(customerDetails) {
        this.setState({actionResult: "PENDING"})
        return fetch('/api/modifyAccount', {
            method: 'PATCH',
            headers: {
                'Accept': 'application/json',
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                accountId: this.state.activeAccountId,
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

    deleteAccount() {
        this.setState({actionResult: "PENDING"})
        return fetch('/api/deleteAccount', {
            method: 'DELETE',
            headers: {
                'Accept': 'application/json',
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                accountId: this.state.activeAccountId
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

    generateBill() {
        this.setState({actionState: "Generate",
                       actionResult: "PENDING"})

        return fetch('/api/submitBillingEntry', {
            method: 'PATCH',
            headers: {
                'Accept': 'application/json',
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                accountId: this.state.activeAccountId
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

    adjustBalance(reason, amount) {
        this.setState({actionResult: "PENDING"})
        amount = amount * -1

        return fetch('/api/submitBillingAdjust', {
            method: 'PATCH',
            headers: {
                'Accept': 'application/json',
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                accountId: this.state.activeAccountId,
                description: reason,
                amount: amount
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

    finishAction() {
        this.setState({currentAction: "",
                       actionResult: ""})
        this.getAccounts()
    }

    render() {
        return (
            <div>
                <NavigationBar nodeInfo={this.state.nodeInfo} />
                {this.state.initialised == true &&
                <AccountList accounts={this.state.accounts}
                             activeAccountId={this.state.activeAccountId}
                             onAccountSelect={this.accountSelect}
                             onAccountCreateRequest={this.accountCreateRequest}
                             onAccountModifyRequest={this.accountModifyRequest}
                             onAccountDeleteRequest={this.accountDeleteRequest}
                             onBillRequest={this.billRequest}
                             onAdjustRequest={this.adjustRequest}
                             onAccountViewRequest={this.accountViewRequest}/>
                }
                <AccountCreateDialog actionState={this.state.currentAction}
                                     actionResult={this.state.actionResult}
                                     onClose={this.finishAction}
                                     onCreateConfirm={(accountDetails) =>
                                         this.createAccount(accountDetails)}/>
                <AccountModifyDialog key={this.state.activeAccountId}
                                     actionState={this.state.currentAction}
                                     actionResult={this.state.actionResult}
                                     accountDetails={this.state.activeAccount}
                                     onClose={this.finishAction}
                                     onModifyConfirm={(accountDetails) =>
                                         this.modifyAccount(accountDetails)}/>
                <AccountDeleteDialog actionState={this.state.currentAction}
                                     actionResult={this.state.actionResult}
                                     accountId={this.state.activeAccountId}
                                     onClose={this.finishAction}
                                     onDeleteConfirm={this.deleteAccount}/>
                <GenerateBillDialog  key={this.state.activeAccountId}
                                     actionState={this.state.currentAction}
                                     actionResult={this.state.actionResult}
                                     accountId={this.state.activeAccountId}
                                     onClose={this.finishAction}
                                     onBillSubmit={this.generateBill} />
                <BalanceAdjustDialog key={this.state.activeAccountId}
                                     actionState={this.state.currentAction}
                                     actionResult={this.state.actionResult}
                                     accountDetails={this.state.activeAccount}
                                     onClose={this.finishAction}
                                     onAdjustSubmit={
                                         (reason, amount) => this.adjustBalance(reason, amount)} />
                <AccountViewDialog   key={this.state.activeAccountId}
                                     actionState={this.state.currentAction}
                                     accountDetails={this.state.activeAccount}
                                     onClose={this.finishAction}/>
            </div>
        )
    }
}
